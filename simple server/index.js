const express = require("express");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// --- Helper de log de domínio ---
function logAction(action, details = {}) {
  const ts = new Date().toISOString();
  console.log(`[${ts}] ${action}:`, JSON.stringify(details, null, 2));
}

// --- Middleware de log de requisição básica ---
app.use((req, res, next) => {
  logAction("HTTP_REQUEST", {
    method: req.method,
    path: req.originalUrl,
    query: req.query,
    // cuidado: body pode ter senha, então não loga em prod real
    body: req.body,
  });
  next();
});

const users = [
  { id: "1", name: "Usuário Exemplo", email: "user@example.com", password: "123456" },
  { id: "2", name: "Alice", email: "alice@example.com", password: "alice123" },
  { id: "3", name: "Bob", email: "bob@example.com", password: "bob123" },
  { id: "4", name: "Carol", email: "carol@example.com", password: "carol123" },
];

const accounts = [
  { id: "acc1", ownerUserId: "1", balanceInCents: 100_000 },
  { id: "acc2", ownerUserId: "2", balanceInCents: 50_000 },
  { id: "acc3", ownerUserId: "3", balanceInCents: 75_000 },
  { id: "acc4", ownerUserId: "4", balanceInCents: 25_000 },
];

function findAccountByUserId(userId) {
  return accounts.find((acc) => acc.ownerUserId === userId);
}

function findAccountById(id) {
  return accounts.find((acc) => acc.id === id);
}

app.post("/auth/login", (req, res) => {
  const { email, password } = req.body || {};

  logAction("LOGIN_ATTEMPT", { email });

  const user = users.find((u) => u.email === email);
  if (!user || user.password !== password) {
    logAction("LOGIN_FAILED", { email, reason: "invalid credentials" });
    return res.status(401).json({ message: "Credenciais inválidas" });
  }

  const token = `fake-token-${user.id}`;

  logAction("LOGIN_SUCCESS", {
    userId: user.id,
    email: user.email,
    token,
  });

  return res.json({
    token,
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
    },
  });
});

app.get("/wallet/summary", (req, res) => {
  const userId = req.query.userId;
  const account = findAccountByUserId(userId);

  if (!account) {
    logAction("SUMMARY_FAILED", { userId, reason: "account not found" });
    return res.status(404).json({ message: "Conta não encontrada" });
  }

  logAction("SUMMARY_FETCHED", {
    userId,
    accountId: account.id,
    balanceInCents: account.balanceInCents,
  });

  res.json({ balanceInCents: account.balanceInCents });
});

app.get("/wallet/contacts", (req, res) => {
  const userId = req.query.userId;
  const contacts = accounts
    .filter((acc) => acc.ownerUserId !== userId)
    .map((acc) => {
      const owner = users.find((u) => u.id === acc.ownerUserId);
      return {
        id: acc.id,
        ownerUserId: acc.ownerUserId,
        name: owner ? owner.name : "Desconhecido",
        accountNumber: `0001-${acc.id.slice(-1)}`,
      };
    });

  logAction("CONTACTS_LISTED", {
    requesterUserId: userId,
    totalContacts: contacts.length,
    contacts: contacts.map((c) => ({
      id: c.id,
      name: c.name,
      accountNumber: c.accountNumber,
    })),
  });

  res.json(contacts);
});

// Somente validações de valor/payer/payee/saldo. Autorização é endpoint separado.
app.post("/wallet/transfer", (req, res) => {
  const { userId, toContactId, amountInCents } = req.body || {};

  logAction("TRANSFER_REQUEST", {
    userId,
    toContactId,
    amountInCents,
  });

  const payer = findAccountByUserId(userId);
  const payee = findAccountById(toContactId);

  if (!payee) {
    logAction("TRANSFER_DENIED", {
      userId,
      toContactId,
      amountInCents,
      reason: "invalid contact",
    });
    return res.status(400).json({ message: "Contato inválido" });
  }

  if (!payer) {
    logAction("TRANSFER_DENIED", {
      userId,
      toContactId,
      amountInCents,
      reason: "payer account not found",
    });
    return res.status(400).json({ message: "Conta não encontrada" });
  }

  if (payer.id === payee.id) {
    logAction("TRANSFER_DENIED", {
      userId,
      toContactId,
      amountInCents,
      reason: "payer equals payee",
    });
    return res.status(400).json({ message: "Payer e payee não podem ser iguais" });
  }

  if (!amountInCents || amountInCents <= 0) {
    logAction("TRANSFER_DENIED", {
      userId,
      toContactId,
      amountInCents,
      reason: "invalid amount",
    });
    return res.status(400).json({ message: "Valor inválido" });
  }

  if (amountInCents > payer.balanceInCents) {
    logAction("TRANSFER_DENIED", {
      userId,
      fromAccountId: payer.id,
      toAccountId: payee.id,
      amountInCents,
      currentBalance: payer.balanceInCents,
      reason: "insufficient funds",
    });
    return res.status(400).json({ message: "Saldo insuficiente" });
  }

  payer.balanceInCents -= amountInCents;
  payee.balanceInCents += amountInCents;

  logAction("TRANSFER_EXECUTED", {
    fromAccountId: payer.id,
    toAccountId: payee.id,
    amountInCents,
    newPayerBalance: payer.balanceInCents,
    newPayeeBalance: payee.balanceInCents,
  });

  return res.json({ balanceInCents: payer.balanceInCents });
});

// Autorização padronizada: 40300 (R$ 403,00) => false + reason
app.post("/authorize", (req, res) => {
  const { value } = req.body || {};
  const authorized = value !== 40_300;

  logAction("AUTH_REQUEST", { value, authorized });

  if (!authorized) {
    return res.json({ authorized: false, reason: "operation not allowed" });
  }
  return res.json({ authorized: true });
});

app.listen(PORT, "0.0.0.0", () => {
  logAction("SERVER_STARTED", { port: PORT, host: "0.0.0.0" });
  console.log(`Wallet API rodando em http://localhost:${PORT}`);
});
