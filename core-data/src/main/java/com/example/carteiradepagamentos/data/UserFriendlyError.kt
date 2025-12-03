package com.example.carteiradepagamentos.data

import java.io.IOException

fun Throwable.toUserFriendlyMessage(): String =
    when (this) {
        is IOException -> "Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente."
        else -> this.message ?: "Erro inesperado. Tente novamente."
    }
