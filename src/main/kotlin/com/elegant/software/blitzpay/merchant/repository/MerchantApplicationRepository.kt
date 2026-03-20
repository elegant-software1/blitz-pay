package com.elegant.software.blitzpay.merchant.repository

import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import java.util.Optional
import java.util.UUID

interface MerchantApplicationRepository {
    fun existsByBusinessProfileRegistrationNumberAndStatusIn(
        registrationNumber: String,
        statuses: Set<MerchantOnboardingStatus>
    ): Boolean

    fun save(application: MerchantApplication): MerchantApplication

    fun findById(merchantId: UUID): Optional<MerchantApplication>
}
