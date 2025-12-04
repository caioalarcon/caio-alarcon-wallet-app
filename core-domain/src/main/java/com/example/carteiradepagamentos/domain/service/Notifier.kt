package com.example.carteiradepagamentos.domain.service

import com.example.carteiradepagamentos.domain.model.Contact

fun interface Notifier {
    fun notifyTransferSuccess(contact: Contact, amountInCents: Long)
}
