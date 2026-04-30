package com.elegant.software.blitzpay.payments.stripe.internal

import com.elegant.software.blitzpay.merchant.api.MerchantCredentialResolver
import com.elegant.software.blitzpay.order.api.OrderGateway
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusInitializationGateway
import com.stripe.exception.StripeException
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

data class CreateIntentRequest(
    val amount: Double? = null,
    val currency: String? = null,
    val orderId: String,
    val merchantId: UUID? = null,
    val branchId: UUID? = null,
    val productId: UUID? = null,
)

data class CreateIntentResponse(
    val clientSecret: String,
    val paymentIntent: String,
    val publishableKey: String,
)
data class ErrorResponse(val error: String)

@RestController
@RequestMapping("/{version:v\\d+(?:\\.\\d+)*}/payments/stripe", version = "1")
class StripePaymentController(
    private val stripePaymentService: StripePaymentService,
    private val credentialResolver: MerchantCredentialResolver,
    private val orderGateway: OrderGateway,
    private val paymentStatusInitializationGateway: PaymentStatusInitializationGateway,
) {
    private val log = LoggerFactory.getLogger(StripePaymentController::class.java)

    @Operation(summary = "Create a Stripe PaymentIntent and return credentials to the mobile SDK.")
    @PostMapping("/create-intent")
    fun createIntent(@RequestBody request: CreateIntentRequest): Mono<ResponseEntity<Any>> {
        val merchantId = request.merchantId
            ?: return Mono.just(
                ResponseEntity.badRequest().body(ErrorResponse("merchantId is required") as Any)
            )
        val orderId = request.orderId.trim().takeIf { it.isNotEmpty() }
            ?: return Mono.just(
                ResponseEntity.badRequest().body(ErrorResponse("orderId must not be blank") as Any)
            )
        val orderSummary = try {
            orderGateway.assertPayable(orderId)
        } catch (ex: NoSuchElementException) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message ?: "Order not found") as Any))
        } catch (ex: IllegalStateException) {
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message ?: "Order is not payable") as Any))
        }
        if (merchantId != orderSummary.merchantId) {
            return Mono.just(ResponseEntity.badRequest().body(ErrorResponse("merchantId does not match the order") as Any))
        }

        // Resolve branch: explicit branchId → product.merchantBranchId → 400
        val resolvedBranchId = credentialResolver.resolveBranch(merchantId, request.branchId ?: orderSummary.branchId, request.productId)
            ?: return Mono.just(
                ResponseEntity.badRequest().body(ErrorResponse("branch cannot be resolved: supply branchId or productId") as Any)
            )

        // Resolve amount: explicit amount overrides product price; log WARN on mismatch
        val resolvedAmount = resolveAmount(request, orderSummary.totalAmountMinor) ?: return Mono.just(
            ResponseEntity.badRequest().body(ErrorResponse("amount must be a positive number") as Any)
        )

        // Resolve Stripe credentials
        val credentials = credentialResolver.resolveStripe(merchantId, resolvedBranchId)
            ?: return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("Payment provider not configured") as Any)
            )

        val currency = request.currency ?: "eur"
        val paymentRequestId = UUID.randomUUID()
        paymentStatusInitializationGateway.initialize(
            paymentRequestId = paymentRequestId,
            payerRef = null,
            orderId = orderId,
            amountMinorUnits = Math.round(resolvedAmount * 100),
            currency = currency.uppercase(),
        )
        orderGateway.linkPaymentAttempt(orderId, paymentRequestId, "STRIPE", null)
        return stripePaymentService.createIntent(
            resolvedAmount,
            currency,
            credentials,
            merchantId,
            resolvedBranchId,
            orderId,
            paymentRequestId,
            request.productId
        )
            .map { result ->
                ResponseEntity.ok(
                    CreateIntentResponse(
                        clientSecret = result.clientSecret,
                        paymentIntent = result.clientSecret,
                        publishableKey = result.publishableKey,
                    ) as Any
                )
            }
            .onErrorResume(IllegalArgumentException::class.java) { ex ->
                Mono.just(ResponseEntity.badRequest().body(ErrorResponse(ex.message ?: "bad request") as Any))
            }
            .onErrorResume(StripeException::class.java) { ex ->
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ErrorResponse(ex.message ?: "Stripe error") as Any)
                )
            }
    }

    private fun resolveAmount(request: CreateIntentRequest, orderAmountMinor: Long): Double? {
        val productPrice = request.productId?.let { credentialResolver.resolveProductPrice(it) }
        val effectiveAmount = when {
            request.amount != null && productPrice != null -> {
                val requestedDouble = request.amount
                val priceDouble = productPrice.toDouble()
                if (requestedDouble != priceDouble) {
                    log.warn(
                        "stripe create_intent amount override productId={} requestAmount={} productPrice={}",
                        request.productId, requestedDouble, priceDouble,
                    )
                }
                requestedDouble
            }
            request.amount != null -> request.amount
            productPrice != null -> productPrice.toDouble()
            else -> orderAmountMinor / 100.0
        }
        return effectiveAmount?.takeIf { it > 0 && it.isFinite() }
    }
}
