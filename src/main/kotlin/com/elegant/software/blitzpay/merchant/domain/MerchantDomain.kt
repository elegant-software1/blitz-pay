package com.elegant.software.blitzpay.merchant.domain

import java.time.Instant
import java.util.UUID

data class MerchantApplication(
    val id: UUID = UUID.randomUUID(),
    val applicationReference: String,
    val businessProfile: BusinessProfile,
    val primaryContact: PrimaryContact,
    val people: List<MerchantPerson> = emptyList(),
    val supportingMaterials: List<MerchantSupportingMaterial> = emptyList(),
    val status: MerchantOnboardingStatus = MerchantOnboardingStatus.DRAFT,
    val submittedAt: Instant? = null,
    val lastUpdatedAt: Instant? = null
) {
    fun registerDirect(registeredAt: Instant): MerchantApplication = copy(
        status = MerchantOnboardingStatus.ACTIVE,
        submittedAt = registeredAt,
        lastUpdatedAt = registeredAt
    )
}

data class BusinessProfile(
    val legalBusinessName: String,
    val businessType: String,
    val registrationNumber: String,
    val operatingCountry: String,
    val primaryBusinessAddress: String
)

data class PrimaryContact(
    val fullName: String,
    val email: String,
    val phoneNumber: String
)

data class MerchantPerson(
    val fullName: String,
    val role: String,
    val countryOfResidence: String,
    val ownershipPercentage: Int? = null
)

data class MerchantSupportingMaterial(
    val type: String,
    val fileName: String,
    val uploadedAt: Instant
)

enum class MerchantOnboardingStatus {
    DRAFT,
    SUBMITTED,
    VERIFICATION,
    SCREENING,
    RISK_REVIEW,
    DECISION_PENDING,
    SETUP,
    ACTIVE,
    MONITORING,
    ACTION_REQUIRED
}
