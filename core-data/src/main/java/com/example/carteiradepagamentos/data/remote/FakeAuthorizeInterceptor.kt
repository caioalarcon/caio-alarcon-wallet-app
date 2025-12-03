package com.example.carteiradepagamentos.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject

class FakeAuthorizeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.method == "POST" && request.url.encodedPath.endsWith("/authorize")) {
            val bodyStr = request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: "{}"

            val json = JSONObject(bodyStr)
            val value = json.optLong("value", 0L)

            val responseJson = if (value == 40300L) {
                """{"authorized":false,"reason":"operation not allowed"}"""
            } else {
                """{"authorized":true}"""
            }

            return Response.Builder()
                .request(request)
                .code(200)
                .protocol(Protocol.HTTP_1_1)
                .message("OK")
                .body(
                    responseJson.toResponseBody(
                        "application/json".toMediaType()
                    )
                )
                .build()
        }

        return chain.proceed(request)
    }
}
