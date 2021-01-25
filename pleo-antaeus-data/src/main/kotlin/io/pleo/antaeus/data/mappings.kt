/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Payment
import io.pleo.antaeus.models.PaymentStatus

import java.time.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)

private fun dateTimeToLocalDate(date: DateTime): LocalDate? {
    val utcDateTime = date.withZone(DateTimeZone.UTC)
    return LocalDate.of(utcDateTime.year, utcDateTime.monthOfYear, utcDateTime.dayOfMonth)
}

fun ResultRow.toPayment(): Payment = Payment(
    customerId = this[PaymentTable.customerId],
    invoiceId = this[PaymentTable.invoiceId],
    lastBilled = dateTimeToLocalDate(this[PaymentTable.lastBilled]),
    paymentStatus = PaymentStatus.valueOf(this[PaymentTable.paymentStatus])
)
