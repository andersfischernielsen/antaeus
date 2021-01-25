package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal
import java.time.LocalDate

/*
These tests show sign of being mainly positive tests (which isn't entirely wrong). I simply wanted to demonstrate
the testing methodology of testing none, a single (singleton) element and and many elements.
Everything the BillingService depends on has been mocked and the service has been isolated to test the service logic
in isolation. These tests can of course be extended for better coverage, especially in negative test cases.
 */

class BillingServiceTest {
    private val invoice1 = Invoice(1, 1, Money(BigDecimal.ONE, Currency.DKK), InvoiceStatus.PENDING)
    private val invoice2 = Invoice(2, 2, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PENDING)
    private val invoice3 = Invoice(3, 3, Money(BigDecimal.TEN, Currency.SEK), InvoiceStatus.PENDING)
    private val invoice4 = Invoice(4, 4, Money(BigDecimal.TEN, Currency.GBP), InvoiceStatus.PAID)
    private val invoice5 = Invoice(5, 5, Money(BigDecimal.TEN, Currency.DKK), InvoiceStatus.PAID)
    private val invoice5repeat = Invoice(5, 5, Money(BigDecimal.TEN, Currency.DKK), InvoiceStatus.PENDING)

    private val payment1 = Payment(1, 1, LocalDate.now(), PaymentStatus.SUCCEEDED)
    private val payment1failed = Payment(1, 1, LocalDate.now(), PaymentStatus.FAILED)
    private val payment2 = Payment(2, 2, LocalDate.now(), PaymentStatus.SUCCEEDED)
    private val payment3 = Payment(3, 3, LocalDate.now(), PaymentStatus.SUCCEEDED)
    private val payment4 = Payment(4, 4, LocalDate.now(), PaymentStatus.FAILED)
    private val payment5 = Payment(5, 5,
            LocalDate.now().minusMonths(1), PaymentStatus.FAILED)
    private val payment6 = Payment(5, 5, LocalDate.now(), PaymentStatus.SUCCEEDED)

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(1) } returns invoice1
        every { fetchInvoice(2) } returns invoice2
        every { fetchInvoice(3) } returns invoice3
        every { fetchInvoice(4) } returns invoice4
        every { fetchInvoice(5) } returns invoice5

        every { updateInvoice(any(), any(), any(), any()) } returns null

        every { createPayment(1, 1, PaymentStatus.SUCCEEDED) } returns payment1
        every { createPayment(1, 1, PaymentStatus.FAILED) } returns payment1failed
        every { createPayment(2, 2, PaymentStatus.SUCCEEDED) } returns payment2
        every { createPayment(3, 3, PaymentStatus.SUCCEEDED) } returns payment3
        every { createPayment(4, 4, PaymentStatus.SUCCEEDED) } returns payment4
        every { createPayment(5, 5, PaymentStatus.FAILED) } returns payment5
        every { createPayment(5, 5, PaymentStatus.SUCCEEDED) } returns payment6

        every { fetchPayment(1) } returns null
        every { fetchPayment(2) } returns null
        every { fetchPayment(3) } returns null
        every { fetchPayment(4) } returns payment4
        every { fetchPayment(5) } returns payment5
    }

    private val failingPaymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns false
    }

    private val succeedingPaymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val withFailingPaymentProvider = BillingService(dal = dal, paymentProvider = failingPaymentProvider)
    private val withSucceedingPaymentProvider = BillingService(dal = dal, paymentProvider = succeedingPaymentProvider)

    @Test
    fun `will not throw on an empty collection of Invoices`() {
        assertDoesNotThrow { withFailingPaymentProvider.billAll(listOf()) }
        assertDoesNotThrow { withSucceedingPaymentProvider.billAll(listOf()) }
    }

    @Test
    fun `will return FAILED Payment on failing PaymentProvider with a single Invoice`() {
        val billed = withFailingPaymentProvider.billAll(listOf(invoice1))
        assert(billed.size == 1)
        assert(billed[0].paymentStatus == PaymentStatus.FAILED)
    }

    @Test
    fun `will return SUCCEEDED Payment on succeeding PaymentProvider with a single Invoice`() {
        val billed = withSucceedingPaymentProvider.billAll(listOf(invoice1))
        assert(billed.size == 1)
        assert(billed[0].paymentStatus == PaymentStatus.SUCCEEDED)
    }

    @Test
    fun `will return SUCCEEDED Payments on succeeding PaymentProvider with multiple PENDING Invoices`() {
        val input = listOf(invoice1, invoice2, invoice3)
        val billed = withSucceedingPaymentProvider.billAll(input)
        assert(billed.size == input.size)
        assert(billed.all { it.paymentStatus == PaymentStatus.SUCCEEDED })
    }

    @Test
    fun `will not return Payment on succeeding PaymentProvider with single PAID Invoice`() {
        val billed = withSucceedingPaymentProvider.billAll(listOf(invoice4))
        assert(billed.isEmpty())
    }

    @Test
    fun `will not return any Payments on succeeding PaymentProvider with multiple PAID Invoices`() {
        val billed = withSucceedingPaymentProvider.billAll(listOf(invoice4, invoice5))
        assert(billed.isEmpty())
    }

    @Test
    fun `will not return Payment if Payment exists for PAID Invoice`() {
        val billedSucceeding = withSucceedingPaymentProvider.billAll(listOf(invoice4, invoice5))
        val billedFailing = withFailingPaymentProvider.billAll(listOf(invoice4, invoice5))
        assert(billedSucceeding.isEmpty())
        assert(billedFailing.isEmpty())
    }

    @Test
    fun `will return Payment for existing SUCCEEDED Payment older than a month and recurring PENDING Invoice`() {
        val billed = withSucceedingPaymentProvider.billAll(listOf(invoice5repeat))
        assert(billed.size == 1)
        assert(billed[0].paymentStatus == PaymentStatus.SUCCEEDED)
    }
}
