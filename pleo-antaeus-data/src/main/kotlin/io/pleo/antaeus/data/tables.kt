/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.CurrentDateTime

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status")
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}

object PaymentTable : Table () {
    val customerId = reference("customer_id", CustomerTable.id).primaryKey()
    val invoiceId = reference("invoice_id", InvoiceTable.id)
    val lastBilled = datetime("lastBilled").defaultExpression(CurrentDateTime())
    val paymentStatus = text("status")
}
