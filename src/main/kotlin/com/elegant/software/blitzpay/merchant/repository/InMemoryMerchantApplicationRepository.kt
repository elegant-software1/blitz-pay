package com.elegant.software.blitzpay.merchant.repository

import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryMerchantApplicationRepository : MerchantApplicationRepository {
    private val applications = ConcurrentHashMap<UUID, MerchantApplication>()

    override fun existsByBusinessProfileRegistrationNumberAndStatusIn(
        registrationNumber: String,
        statuses: Set<MerchantOnboardingStatus>
    ): Boolean = applications.values.any { application ->
        application.businessProfile.registrationNumber == registrationNumber && application.status in statuses
    }

    override fun save(application: MerchantApplication): MerchantApplication {
        applications[application.id] = application
        return application
    }

    override fun findById(merchantId: UUID): Optional<MerchantApplication> =
        Optional.ofNullable(applications[merchantId])
}
