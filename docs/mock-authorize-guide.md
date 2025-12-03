# Guia rápido: mock `/authorize` e prontidão para backend real

Este passo a passo foi pensado para alguém sem contexto prévio conseguir deixar o app funcionando em **debug** com mock embutido e preparado para **produção** quando existir um backend real. Cada bloco indica o que alterar e onde encontrar os arquivos.

## Objetivo em uma frase

- **Debug/teste:** `POST /authorize` feito via Retrofit, respondido por um `FakeAuthorizeInterceptor` em memória.
- **Produção:** o mesmo código passa a usar um servidor real apenas trocando `API_BASE_URL` no `build.gradle.kts` (sem mexer em código Kotlin).

## 1. Dependências (ex.: `app/build.gradle.kts`)

Adicione as bibliotecas de rede onde fica o código Android (normalmente módulo `app`). Versões podem ser ajustadas conforme o projeto, o importante é ter OkHttp + Retrofit + Moshi:

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
}
```

## 2. `API_BASE_URL` por build type

No mesmo `build.gradle.kts`, configure `BuildConfig` para debug e release. Em debug o domínio serve só de placeholder, porque o interceptor responde localmente.

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://example.com/api/v1/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://example.com/api/v1/\"")
        }
    }
}
```

Quando houver backend real, basta trocar o valor em `release` (e, se quiser, no `debug`).

## 3. Contrato HTTP `/authorize`

Crie a interface e os DTOs em `app/src/main/java/com/example/carteiradepagamentos/data/remote/AuthorizeApi.kt` (ajuste o caminho conforme o pacote):

```kotlin
package com.example.carteiradepagamentos.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class AuthorizeRequest(val value: Double)

data class AuthorizeResponse(val authorized: Boolean, val reason: String? = null)

interface AuthorizeApi {
    @POST("authorize")
    suspend fun authorize(@Body request: AuthorizeRequest): AuthorizeResponse
}
```

- Request esperado: `{ "value": 100.0 }`
- Sucesso: `{ "authorized": true }`
- Regra especial: se `value == 403.0`, resposta deve ser `{ "authorized": false, "reason": "operation not allowed" }`.

## 4. `FakeAuthorizeInterceptor`

Implemente um interceptor OkHttp que responde localmente a `POST /authorize`. Exemplo em `app/src/main/java/.../data/remote/FakeAuthorizeInterceptor.kt`:

```kotlin
class FakeAuthorizeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method == "POST" && request.url.encodedPath.endsWith("/authorize")) {
            val bodyStr = chain.request().body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: "{}"

            val value = JSONObject(bodyStr).optDouble("value", 0.0)
            val responseJson = if (value == 403.0) {
                """{"authorized":false,"reason":"operation not allowed"}"""
            } else {
                """{"authorized":true}"""
            }

            return Response.Builder()
                .request(request)
                .code(200)
                .protocol(Protocol.HTTP_1_1)
                .message("OK")
                .body(responseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }
        return chain.proceed(request)
    }
}
```

Assim, o app roda offline em debug e respeita a regra do valor `403` diretamente no mock (não na camada de UI/domínio).

## 5. Módulo de rede (Hilt)

Em `app/src/main/java/com/example/carteiradepagamentos/di/NetworkModule.kt`, configure Retrofit/OkHttp e injete o interceptor apenas em debug:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) builder.addInterceptor(FakeAuthorizeInterceptor())
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthorizeApi(retrofit: Retrofit): AuthorizeApi = retrofit.create(AuthorizeApi::class.java)
}
```

- Em **debug**, o interceptor intercepta e responde; nenhuma requisição sai do app.
- Em **release**, o cliente usa `API_BASE_URL` e fala com o backend real (quando existir).

## 6. Serviço de domínio via rede

Adapte a implementação de `AuthorizeService` para usar a API HTTP. Exemplo em `data/remote/NetworkAuthorizeService.kt`:

```kotlin
class NetworkAuthorizeService @Inject constructor(
    private val api: AuthorizeApi
) : AuthorizeService {
    override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> = try {
        val value = amountInCents / 100.0
        val response = api.authorize(AuthorizeRequest(value))
        Result.success(response.authorized)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

Mantém a conversão de centavos para `Double` e retorna `Result<Boolean>` como o serviço atual.

## 7. Bind no `AppModule`

Substitua o bind atual do `AuthorizeService` para apontar para `NetworkAuthorizeService` (arquivo `app/src/main/java/.../di/AppModule.kt`):

```kotlin
@Binds
@Singleton
abstract fun bindAuthorizeService(impl: NetworkAuthorizeService): AuthorizeService
```

O mock HTTP permanece transparente para o domínio: em debug, é interceptado; em produção, segue para o backend.

## 8. Notificação local após autorização

A arquitetura já prevê um `Notifier` (ex.: `AndroidNotifier`). Garanta que a implementação exibe uma push local depois de `authorizeTransfer` retornar sucesso. Não há necessidade de retry ou rollback.

## 9. Checklist final

- [ ] Dependências de rede adicionadas ao módulo Android.
- [ ] `API_BASE_URL` configurado por build type.
- [ ] `AuthorizeApi` + DTOs criados.
- [ ] `FakeAuthorizeInterceptor` responde a `POST /authorize` (regra do `403`).
- [ ] `NetworkModule` injeta Retrofit/OkHttp com interceptor apenas em debug.
- [ ] `NetworkAuthorizeService` substitui o fake na ligação do Hilt.
- [ ] Notificação local continua sendo chamada após sucesso.

Seguindo estes passos, o app fica pronto para testar o fluxo completo offline e já preparado para apontar para um backend real no futuro apenas alterando a base URL.
