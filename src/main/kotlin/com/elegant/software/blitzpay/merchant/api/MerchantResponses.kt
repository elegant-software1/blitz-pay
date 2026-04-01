package com.elegant.software.blitzpay.merchant.api

import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import java.time.Instant
import java.util.UUID

data class MerchantApplicationResponse(
    val applicationId: UUID,
    val applicationReference: String,
    val status: MerchantOnboardingStatus,
    val businessProfile: MerchantBusinessProfileResponse,
    val primaryContact: MerchantPrimaryContactResponse,
    val people: List<MerchantPersonResponse>,
    val supportingMaterials: List<MerchantSupportingMaterialResponse>,
    val submittedAt: Instant?,
    val lastUpdatedAt: Instant?
)

data class MerchantBusinessProfileResponse(
    val legalBusinessName: String,
    val businessType: String,
    val registrationNumber: String,
    val operatingCountry: String,
    val primaryBusinessAddress: String
)

data class MerchantPrimaryContactResponse(
    val fullName: String,
    val email: String,
    val phoneNumber: String
)

data class MerchantPersonResponse(
    val fullName: String,
    val role: String,
    val countryOfResidence: String,
    val ownershipPercentage: Int?
)

data class MerchantSupportingMaterialResponse(
    val type: String,
    val fileName: String,
    val uploadedAt: Instant
)
