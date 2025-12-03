# Carteira de Pagamentos

Aplicativo Android em Jetpack Compose com arquitetura modular para autenticação, consulta de saldo e transferências. Este guia resume como rodar, testar e entender as principais decisões do projeto.

## Como rodar o app
1. Instale o Android SDK e defina `sdk.dir` em `local.properties` (ex.: `/home/user/Android/Sdk`).
2. Use o Java 11+ (o wrapper já baixa o Gradle 8.13).
3. Sincronize o projeto no Android Studio Hedgehog ou superior.
4. Escolha o build variant **debug** e rode no emulador/dispositivo (minSdk 24, targetSdk 36).

## Como rodar os testes
- **Unit tests**: `./gradlew test --console=plain --no-daemon`
  - Em ambientes sem SDK configurado, o Gradle falhará informando que o `sdk.dir`/`ANDROID_HOME` não foi encontrado (comportamento esperado em CI ou containers sem SDK).

## Versões de build e dependências principais
- **Gradle wrapper**: 8.13. 【F:gradle/wrapper/gradle-wrapper.properties†L1-L8】
- **Android Gradle Plugin**: 8.13.1. 【F:gradle/libs.versions.toml†L1-L14】
- **Kotlin**: 2.0.21. 【F:gradle/libs.versions.toml†L1-L14】
- **SDK**: compile/target 36, minSdk 24. 【F:app/build.gradle.kts†L10-L33】
- **Principais libs**: Compose BOM 2024.09.00, Material3, Activity Compose, Lifecycle, Coroutines, Hilt, Retrofit 2.11.0, OkHttp 4.12.0, Moshi 1.15.1. 【F:app/build.gradle.kts†L49-L77】【F:core-data/build.gradle.kts†L47-L62】

## Decisões arquiteturais
- **Modularização por camada**: `core-domain` (modelos/contratos puros), `core-data` (repos, integrações e mocks), `app` (configuração DI), `feature-*` (telas de login, home e transferência). 【F:app/build.gradle.kts†L62-L66】【F:app/src/main/java/com/example/carteiradepagamentos/di/AppModule.kt†L28-L74】
- **Injeção de dependências**: Hilt centraliza binds em `AppModule` (repos, storage, notifier, serviços). 【F:app/src/main/java/com/example/carteiradepagamentos/di/AppModule.kt†L28-L74】
- **Rede mockada**: `NetworkModule` cria Retrofit apontando para o servidor Node local; o interceptor `FakeAuthorizeInterceptor` está disponível para debug se quiser rodar offline. 【F:app/src/main/java/com/example/carteiradepagamentos/di/NetworkModule.kt†L17-L43】【F:core-data/src/main/java/com/example/carteiradepagamentos/data/remote/FakeAuthorizeInterceptor.kt†L11-L47】
- **Serviço de autorização**: `NetworkAuthorizeService` envia o valor em centavos para `/authorize`, encapsulando falhas em `Result`. 【F:core-data/src/main/java/com/example/carteiradepagamentos/data/remote/NetworkAuthorizeService.kt†L7-L20】
- **Auto-login com token salvo**: `SharedPrefsAuthStorage` persiste `Session`; o `AppViewModel` inicia verificando `authRepository.getCurrentSession()` e decide entre Login e Home sem tela intermediária. 【F:core-data/src/main/java/com/example/carteiradepagamentos/data/local/SharedPrefsAuthRepository.kt†L14-L40】【F:app/src/main/java/com/example/carteiradepagamentos/AppViewModel.kt†L17-L47】

## Mocks HTTP e contrato `/authorize`
- O servidor local fica em `simple server` (`npm start`), com endpoints `/auth/login`, `/wallet/summary`, `/wallet/contacts`, `/wallet/transfer` e **`/authorize`**.
- `/authorize` segue o contrato da especificação: `POST /authorize { "value": <centavos> } → { "authorized": true }`; para `40300` retorna `{ "authorized": false, "reason": "operation not allowed" }`. 【F:simple server/index.js†L122-L144】
- `GET /wallet/contacts` devolve `ownerUserId` de cada conta, garantindo que o app bloqueie payer = payee na camada de apresentação. 【F:simple server/index.js†L67-L88】

## Como reproduzir o cenário do valor 403
1. No fluxo de transferência, informe o valor **R$ 403,00** (campo aceita centavos, portanto `40300`).
2. O endpoint `/authorize` do servidor (ou o `FakeAuthorizeInterceptor`, se ativado) devolve `authorized=false` com razão `operation not allowed`, e o `TransferViewModel` exibe a mensagem "Transferência bloqueada por política de segurança (valor R$ 403,00)". 【F:simple server/index.js†L122-L144】【F:core-data/src/main/java/com/example/carteiradepagamentos/data/remote/FakeAuthorizeInterceptor.kt†L16-L45】【F:feature-transfer/src/main/java/com/example/carteiradepagamentos/feature/transfer/TransferViewModel.kt†L62-L113】

## Validações da transferência e push local
- Validações aplicadas: contato selecionado, valor > 0, saldo suficiente, **payer ≠ payee** (usa `ownerUserId` do contato e o `user.id` da sessão atual). 【F:feature-transfer/src/main/java/com/example/carteiradepagamentos/feature/transfer/TransferViewModel.kt†L70-L123】
- Após autorização, o `AndroidNotifier` dispara uma notificação local formatada; em API 33+, a permissão `POST_NOTIFICATIONS` é declarada e solicitada automaticamente ao abrir a tela de transferência. 【F:app/src/main/AndroidManifest.xml†L3-L7】【F:feature-transfer/src/main/java/com/example/carteiradepagamentos/feature/transfer/TransferScreen.kt†L28-L67】【F:core-data/src/main/java/com/example/carteiradepagamentos/data/notification/AndroidNotifier.kt†L15-L52】

## TODOs de prints
- TODO: Adicionar print/GIF da Home.
- TODO: Adicionar print/GIF da tela de Transferência.
