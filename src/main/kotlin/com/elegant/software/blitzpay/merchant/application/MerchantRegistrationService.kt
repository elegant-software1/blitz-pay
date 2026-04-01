package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.api.RegisterMerchantRequest
import com.elegant.software.blitzpay.merchant.domain.BusinessProfile
import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import com.elegant.software.blitzpay.merchant.domain.PrimaryContact
import com.elegant.software.blitzpay.merchant.repository.MerchantApplicationRepository
import com.elegant.software.blitzpay.merchant.support.MerchantObservabilitySupport
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class MerchantRegistrationService(
    private val merchantApplicationRepository: MerchantApplicationRepository,
    private val merchantAuditTrail: MerchantAuditTrail,
    private val merchantObservabilitySupport: MerchantObservabilitySupport
) {

    fun register(request: RegisterMerchantRequest): MerchantApplication {
        val registrationNumber = request.businessProfile.registrationNumber
        val duplicateExists = merchantApplicationRepository.existsByBusinessProfileRegistrationNumberAndStatusIn(
            registrationNumber,
            ACTIVE_STATUSES
        )
        require(!duplicateExists) {
            "An active merchant application already exists for registration number $registrationNumber"
        }

        val registeredAt = Instant.now()
        val application = MerchantApplication(
            applicationReference = generateReference(),
            businessProfile = BusinessProfile(
                legalBusinessName = request.businessProfile.legalBusinessName,
                businessType = request.businessProfile.businessType,
                registrationNumber = registrationNumber,
                operatingCountry = request.businessProfile.operatingCountry,
                primaryBusinessAddress = request.businessProfile.primaryBusinessAddress
            ),
            primaryContact = PrimaryContact(
                fullName = request.primaryContact.fullName,
                email = request.primaryContact.email,
                phoneNumber = request.primaryContact.phoneNumber
            )
        ).registerDirect(registeredAt)

        val saved = merchantApplicationRepository.save(application)
        merchantObservabilitySupport.recordSuccess("register", MerchantOnboardingStatus.ACTIVE)
        merchantAuditTrail.record(
            MerchantAuditEvent(
                applicationId = saved.id,
                applicationReference = saved.applicationReference,
                actorId = SYSTEM_ACTOR,
                action = "register",
                status = saved.status,
                occurredAt = registeredAt
            )
        )
        return saved
    }

    @Transactional(readOnly = true)
    fun findById(merchantId: UUID): MerchantApplication =
        merchantApplicationRepository.findById(merchantId)
            .orElseThrow { NoSuchElementException("Merchant not found: $merchantId") }

    private fun generateReference(): String =
        "BLTZ-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

    companion object {
        private const val SYSTEM_ACTOR = "system"
        private val ACTIVE_STATUSES = setOf(
            MerchantOnboardingStatus.SUBMITTED,
            MerchantOnboardingStatus.VERIFICATION,
            MerchantOnboardingStatus.SCREENING,
            MerchantOnboardingStatus.RISK_REVIEW,
            MerchantOnboardingStatus.DECISION_PENDING,
            MerchantOnboardingStatus.SETUP,
            MerchantOnboardingStatus.ACTIVE,
            MerchantOnboardingStatus.MONITORING,
            MerchantOnboardingStatus.ACTION_REQUIRED
        )
    }
}
