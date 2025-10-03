package com.elegant.software.quickpay.payments.truelayer.outbound

import com.elegant.software.quickpay.payments.truelayer.api.PaymentGateway
import com.elegant.software.quickpay.payments.truelayer.support.TrueLayerProperties
import com.truelayer.java.TrueLayerClient
import com.truelayer.java.entities.CurrencyCode
import com.truelayer.java.entities.ProviderFilter
import com.truelayer.java.entities.User
import com.truelayer.java.http.entities.ApiResponse
import com.truelayer.java.payments.entities.*
import com.truelayer.java.payments.entities.paymentmethod.PaymentMethod
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import com.truelayer.java.payments.entities.beneficiary.MerchantAccount;
import com.truelayer.java.payments.entities.providerselection.ProviderSelection;
import java.util.Collections

/**
 * TrueLayer payment gateway adapter.
 *
 * For now, we DO NOT perform any outbound API call. Instead, we:
 *  - Build a request-shaped object equivalent to TrueLayer's CreatePaymentRequest
 *  - Log it for traceability
 *  - Return a deterministic placeholder payment id
 *
 * This can be swapped later with the official TrueLayer Java SDK call, using the
 * same fields assembled below.
 */
@Component
class PaymentService(
    private val trueLayerProperties: TrueLayerProperties,
    private val trueLayerClient: TrueLayerClient
) : PaymentGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun startPayment(cmd: PaymentGateway.PaymentRequested): String {
        // Adapt types for SDK requirements
        val amountInMinor = cmd.amountMinorUnits.toInt() // SDK likely expects Int
        val currency = CurrencyCode.valueOf(cmd.currency.uppercase()) // Use SDK's CurrencyCode enum
        val paymentRequest = CreatePaymentRequest.builder()
            .amountInMinor(amountInMinor)
            .currency(currency)
            .paymentMethod(
                PaymentMethod.bankTransfer()
                    .providerSelection(
                        ProviderSelection.userSelected()
                            .filter(
                                ProviderFilter.builder()
                                    .countries(Collections.singletonList(CountryCode.GB))
                                    .releaseChannel(ReleaseChannel.GENERAL_AVAILABILITY)
                                    .customerSegments(Collections.singletonList(CustomerSegment.RETAIL))
                                    .providerIds(Collections.singletonList("mock-payments-gb-redirect"))
                                    .build()
                            )
                            .build()
                    )
                    .beneficiary(
                        MerchantAccount.builder()
                            .merchantAccountId(trueLayerProperties.merchantAccountId)
                            .build()
                    )
                    .build()
            )
            .user(
                User.builder()
                    .name("Andrea")
                    .email("andrea@truelayer.com")
                    .build()
            )
            .build()

        log.info("[DEBUG_LOG] Prepared TrueLayer CreatePaymentRequest: {}", paymentRequest)

        // fire the request
        val paymentResponse: CompletableFuture<ApiResponse<CreatePaymentResponse?>?> = trueLayerClient
            .payments()
            .createPayment(paymentRequest)

        // wait for the response
        val apiResponse = paymentResponse.get()
        if (apiResponse == null) {
            log.error("TrueLayer payment API response was null")
            throw IllegalStateException("Payment API response was null")
        }
        if (apiResponse.error != null) {
            log.error("TrueLayer payment API call failed: {}", apiResponse.error)
            throw IllegalStateException("Payment API call failed: ${apiResponse.error}")
        }
        val paymentData = apiResponse.data
        if (paymentData == null) {
            log.error("TrueLayer payment API response data was null")
            throw IllegalStateException("Payment API response data was null")
        }
        val paymentId = paymentData.id
        if (paymentId == null) {
            log.error("TrueLayer payment ID was null in response")
            throw IllegalStateException("Payment ID was null in response")
        }
        return paymentId
    }
}