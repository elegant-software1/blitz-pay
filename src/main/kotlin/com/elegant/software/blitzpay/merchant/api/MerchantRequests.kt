package com.elegant.software.blitzpay.merchant.api

data class RegisterMerchantRequest(
    val businessProfile: MerchantBusinessProfileRequest,
    val primaryContact: MerchantPrimaryContactRequest
)

data class MerchantBusinessProfileRequest(
    val legalBusinessName: String,
    val businessType: String,
    val registrationNumber: String,
    val operatingCountry: String,
    val primaryBusinessAddress: String
)

data class MerchantPrimaryContactRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String
)
