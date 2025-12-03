const express = require("express");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

const users = [
  { id: "1", name: "Usuário Exemplo", email: "user@example.com", password: "123456" },
  { id: "2", name: "Alice", email: "alice@example.com", password: "alice123" },
  { id: "3", name: "Bob", email: "bob@example.com", password: "bob123" },
  { id: "4", name: "Carol", email: "carol@example.com", password: "carol123" },
];

const accounts = [
  { id: "acc1", userId: "1", balanceInCents: 100_000 },
  { id: "acc2", userId: "2", balanceInCents: 50_000 },
  { id: "acc3", userId: "3", balanceInCents: 75_000 },
  { id: "acc4", userId: "4", balanceInCents: 120_000 },
];

function findAccountByUserId(userId) {
  return accounts.find((acc) => acc.userId === userId);
}

function findAccountById(id) {
  return accounts.find((acc) => acc.id === id);
}

app.post("/auth/login", (req, res) => {
  const { email, password } = req.body || {};
  const user = users.find((u) => u.email === email);
  if (!user || user.password !== password) {
    return res.status(401).json({ message: "Credenciais inválidas" });
  }

  return res.json({
    token: `fake-token-${user.id}`,
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
    return res.status(404).json({ message: "Conta não encontrada" });
  }
  res.json({ balanceInCents: account.balanceInCents });
});

app.get("/wallet/contacts", (req, res) => {
  const userId = req.query.userId;
  const contacts = accounts
    .filter((acc) => acc.userId !== userId)
    .map((acc) => {
      const owner = users.find((u) => u.id === acc.userId);
      return {
        id: acc.id,
        name: owner ? owner.name : "Desconhecido",
        accountNumber: `0001-${acc.id.slice(-1)}`,
      };
    });

  res.json(contacts);
});

app.post("/wallet/transfer", (req, res) => {
  const { userId, toContactId, amountInCents } = req.body || {};

  const payer = findAccountByUserId(userId);
  const payee = findAccountById(toContactId);

  if (!payer || !payee) {
    return res.status(400).json({ message: "Contato inválido" });
  }

  if (payer.id === payee.id) {
    return res.status(400).json({ message: "Payer e payee não podem ser iguais" });
  }

  if (!amountInCents || amountInCents <= 0) {
    return res.status(400).json({ message: "Valor inválido" });
  }

  if (amountInCents > payer.balanceInCents) {
    return res.status(400).json({ message: "Saldo insuficiente" });
  }

  payer.balanceInCents -= amountInCents;
  payee.balanceInCents += amountInCents;

  return res.json({ balanceInCents: payer.balanceInCents });
});

app.post("/authorize", (req, res) => {
  const { value } = req.body || {};
  const authorized = value !== 40_300;
  res.json({ authorized });
});

app.listen(PORT, () => {
  console.log(`Wallet API rodando em http://localhost:${PORT}`);
});
