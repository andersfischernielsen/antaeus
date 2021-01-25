/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Payment
import io.pleo.antaeus.models.PaymentStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.date
import java.time.LocalDate

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoice(id: Int, amount: Money, customerId: Int, status: InvoiceStatus): Invoice? {
        transaction(db) {
            // Update the invoice and returns its id.
            InvoiceTable.update ({ InvoiceTable.id.eq(id) }) {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customerId
                }
        }

        return fetchInvoice(id)
    }

    fun createPayment(customerId: Int, invoiceId: Int, status: PaymentStatus = PaymentStatus.FAILED): Payment? {
        transaction(db) {
            // Insert the payment and returns its new id.
            PaymentTable
                .insert {
                    it[this.customerId] = customerId
                    it[this.invoiceId] = invoiceId
                    it[this.paymentStatus] = status.toString()
                }
        }

        return fetchPayment(customerId)
    }

    fun fetchPayment(id: Int): Payment? {
        return transaction(db) {
            PaymentTable
                .select { PaymentTable.customerId.eq(id) }
                .firstOrNull()
                ?.toPayment()
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
