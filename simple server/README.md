# Simple Server (Wallet API)

API mock em Node/Express usada pelo app de carteira para desenvolvimento local. Não há persistência: os saldos e dados retornam ao estado inicial sempre que o servidor reinicia.

## Como rodar
- Requisitos: Node 18+ e npm.
- Instalação (uma vez): `npm install`
- Execução: `npm start` (define `PORT` para customizar a porta; padrão `3000`).
- A API ficará disponível em `http://localhost:<PORT>`.

## Endpoints rápidos
- `POST /auth/login` — Autentica usuário mock e devolve token fake + dados do usuário.
- `GET /wallet/summary?userId={id}` — Retorna o saldo em centavos da conta do usuário.
- `GET /wallet/contacts?userId={id}` — Lista contas de outros usuários para transferências.
- `POST /wallet/transfer` — Valida e transfere saldo entre contas mock (`userId`, `toContactId`, `amountInCents`).
- `POST /authorize` — Autoriza um valor; `value = 40300` sempre retorna `authorized=false`.

## Dados mockados
- Usuários: `1` (Usuário Exemplo, user@example.com / 123456), `2` (Alice, alice@example.com / alice123), `3` (Bob, bob123), `4` (Carol, carol123).
- Contas: `acc1`→user `1` (100_000), `acc2`→`2` (50_000), `acc3`→`3` (75_000), `acc4`→`4` (25_000). O saldo é mutado em memória durante transfers.

## Logs
Cada requisição é registrada no console com método, path, query e body, além de logs de domínio (login, resumo, transferências e autorização).

## Especificação OpenAPI
O contrato da API está em `openapi.yaml` nesta mesma pasta. Use-o para gerar clientes, mocks ou documentação HTML se preferir.
