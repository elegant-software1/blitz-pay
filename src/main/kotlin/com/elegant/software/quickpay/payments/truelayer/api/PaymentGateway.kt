package com.elegant.software.quickpay.payments.truelayer.api

import org.springframework.modulith.NamedInterface

@NamedInterface("PaymentGateway")
interface PaymentGateway {
    fun startPayment(cmd: PaymentRequested): String

    data class PaymentRequested(
        val orderId: String,
        val amountMinorUnits: Long,
        val currency: String,
        val userDisplayName: String,
        val redirectReturnUri: String
    )
}