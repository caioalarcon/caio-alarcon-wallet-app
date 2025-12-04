# Testes Exploratorios – Carteira de Pagamentos

## Login
- Autenticar com credenciais válidas (offline e servidor ativo) leva à Home.
- Senha inválida exibe erro amigável, permanece na tela.
- Base URL inválida ou servidor offline: erro de rede visível, app não quebra.

## Home (Resumo e Contatos)
- Saldo e contatos carregam após login; recarregar pós-transferência reflete saldo atualizado.
- Sem sessão (reabrir app pós-logout) deve forçar volta ao login.

## Configurações – Tema
- Alternar Claro / Escuro / Sistema e voltar; persistência ao reiniciar app.

## Configurações – Rede
- Switch “Servidor HTTP mock” desligado: usa dados em memória; nenhuma chamada HTTP (transferências funcionam offline).
- Ligado: usar base URL informada; salvar com campo vazio preenche `http://192.168.1.110:3000/`.
- Base URL incorreta ou servidor fora: fluxos (login/transferência) mostram erro de rede sem travar UI.

## Logout
- “Desconectar” limpa sessão e volta ao login; reabrir app continua deslogado.

## Transferência (happy path)
- Offline (mock in-memory): escolher contato, valor >0 e <= saldo, diferente de 40300 → sucesso, saldo diminui, diálogo de sucesso fecha, volta para Home; notificação local emitida.
- Servidor ativo: mesmo fluxo consumindo backend real; notificação também emitida.

## Transferência – Validações
- Valor 0/negativo bloqueia com mensagem.
- Nenhum contato selecionado bloqueia.
- Transferir para si mesmo mostra erro (payer = payee).
- Valor acima do saldo: “Saldo insuficiente”.
- Valor 40300 (R$ 403,00) → backend/autorizer responde `authorized=false`; app exibe mensagem de política e não altera saldo, sem notificação.

## Notificações
- API 33+: permissão `POST_NOTIFICATIONS` solicitada ao abrir tela de transferência.
- Após transferência bem-sucedida, notificação “Transferência realizada” com valor e nome do contato aparece; canal é criado sem crash mesmo após matar/reabrir o app.
