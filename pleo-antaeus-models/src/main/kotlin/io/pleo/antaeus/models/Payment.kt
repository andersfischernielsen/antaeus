package io.pleo.antaeus.models

import java.time.LocalDate

data class Payment(
    val customerId: Int,
    val invoiceId: Int,
    val lastBilled: LocalDate?,
    val paymentStatus: PaymentStatus
)
