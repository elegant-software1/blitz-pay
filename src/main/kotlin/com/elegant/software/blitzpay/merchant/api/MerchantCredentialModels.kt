package com.elegant.software.blitzpay.merchant.api

data class StripeCredentials(
    val secretKey: String,
    val publishableKey: String,
)

data class BraintreeCredentials(
    val merchantId: String,
    val publicKey: String,
    val privateKey: String,
    val environment: String,
)
