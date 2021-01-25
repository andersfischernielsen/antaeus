package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Payment
import io.pleo.antaeus.models.PaymentStatus
import java.time.LocalDate

class BillingService(
    private val paymentProvider: PaymentProvider, 
    private val dal: AntaeusDal
) {
    //Attempt to bill all pending invoices passed as a parameter. 
    //This will only bill customers if the last run was >= a month ago.
    fun billAll(invoices: List<Invoice>): List<Payment> {
        fun isAMonthInThePast(date: LocalDate?) =
                date?.isBefore(LocalDate.now().minusMonths(1).plusDays(1)) ?: true

            return invoices
                    .filter {
                        it.status != InvoiceStatus.PAID && isAMonthInThePast(dal.fetchPayment(it.customerId)?.lastBilled)
                    }
                    //This map could be turned into a pmap depending on the rate limiting/batching
                    //capabilities of the PaymentProvider.
                    .map {
                        val successful = paymentProvider.charge(it)
                        it.copy(status = if (successful) InvoiceStatus.PAID else InvoiceStatus.PENDING)
                    }
                    //This could be batched for speed gains in insertions.
                    .mapNotNull {
                        dal.updateInvoice(it.id, it.amount, it.customerId, it.status)
                        val paymentStatus = when (it.status) {
                            InvoiceStatus.PAID      -> PaymentStatus.SUCCEEDED
                            InvoiceStatus.PENDING   -> PaymentStatus.FAILED
                        }
                        dal.createPayment(it.customerId, it.id, paymentStatus)
                    }

    }
}
