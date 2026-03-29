package com.elegant.software.blitzpay.payments.braintree.internal

import com.braintreegateway.ClientTokenRequest
import com.braintreegateway.TransactionRequest
import com.elegant.software.blitzpay.merchant.api.BraintreeCredentials
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

sealed class BraintreeCheckoutResult {
    data class Success(
        val transactionId: String,
        val amount: String,
        val currency: String,
        val invoiceId: String? = null,
        val merchantId: UUID,
        val branchId: UUID?,
    ) : BraintreeCheckoutResult()

    data class Failure(val message: String, val code: String?) : BraintreeCheckoutResult()
}

@Service
class BraintreePaymentService(private val gatewayFactory: BraintreeGatewayFactory) {

    private val log = LoggerFactory.getLogger(BraintreePaymentService::class.java)

    fun generateClientToken(credentials: BraintreeCredentials): Mono<String> =
        Mono.fromCallable {
            val token = gatewayFactory.get(credentials).clientToken().generate(ClientTokenRequest())
            log.info("braintree client_token issued merchantId={}", credentials.merchantId)
            token
        }.subscribeOn(Schedulers.boundedElastic())

    fun checkout(
        nonce: String,
        amount: Double,
        currency: String,
        credentials: BraintreeCredentials,
        merchantId: UUID,
        branchId: UUID?,
        productId: UUID? = null,
        invoiceId: String? = null,
    ): Mono<BraintreeCheckoutResult> = Mono.fromCallable {
        require(nonce.isNotBlank()) { "nonce is required" }
        require(amount > 0 && amount.isFinite()) { "amount must be a positive number" }
        val formattedAmount = "%.2f".format(Locale.US, amount)
        // productId is the primary external reference; fall back to invoiceId
        val externalRef = productId?.toString() ?: invoiceId ?: ""
        val request = TransactionRequest()
            .amount(BigDecimal(formattedAmount))
            .paymentMethodNonce(nonce)
            .orderId(externalRef)
            .options().submitForSettlement(true).done()
        val result = gatewayFactory.get(credentials).transaction().sale(request)
        if (result.isSuccess && result.target != null) {
            val tx = result.target
            log.info(
                "braintree checkout OK tx={} amount={} currency={} merchantId={} branchId={} productId={} invoice={}",
                tx.id, formattedAmount, currency, merchantId, branchId, productId, invoiceId ?: "n/a",
            )
            BraintreeCheckoutResult.Success(tx.id, formattedAmount, currency, invoiceId, merchantId, branchId)
        } else {
            val tx = result.transaction
            val msg = tx?.processorResponseText ?: result.message ?: "Braintree declined the transaction"
            val code = tx?.processorResponseCode
            log.warn(
                "braintree checkout FAILED code={} message={} merchantId={} branchId={} invoice={}",
                code ?: "n/a", msg, merchantId, branchId, invoiceId ?: "n/a",
            )
            BraintreeCheckoutResult.Failure(msg, code)
        }
    }.subscribeOn(Schedulers.boundedElastic())
}
