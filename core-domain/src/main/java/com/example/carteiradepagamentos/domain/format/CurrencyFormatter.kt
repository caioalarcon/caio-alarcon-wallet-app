package com.example.carteiradepagamentos.domain.format

import java.text.NumberFormat
import java.util.Locale

fun Long.toBRCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this / 100.0).replace('\u00A0', ' ')
}
