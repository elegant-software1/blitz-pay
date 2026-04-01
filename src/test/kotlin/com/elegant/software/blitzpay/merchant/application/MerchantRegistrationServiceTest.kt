package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.api.MerchantBusinessProfileRequest
import com.elegant.software.blitzpay.merchant.api.MerchantPrimaryContactRequest
import com.elegant.software.blitzpay.merchant.api.RegisterMerchantRequest
import com.elegant.software.blitzpay.merchant.domain.BusinessProfile
import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import com.elegant.software.blitzpay.merchant.domain.PrimaryContact
import com.elegant.software.blitzpay.merchant.repository.MerchantApplicationRepository
import com.elegant.software.blitzpay.merchant.support.MerchantObservabilitySupport
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MerchantRegistrationServiceTest {

    private val repository = mock<MerchantApplicationRepository>()
    private val auditTrail = mock<MerchantAuditTrail>()
    private val observabilitySupport = mock<MerchantObservabilitySupport>()
    private val service = MerchantRegistrationService(repository, auditTrail, observabilitySupport)

    @Test
    fun `register creates ACTIVE merchant and persists it`() {
        whenever(
            repository.existsByBusinessProfileRegistrationNumberAndStatusIn(eq("DE123"), any())
        ).thenReturn(false)
        whenever(repository.save(any<MerchantApplication>())).thenAnswer { it.getArgument<MerchantApplication>(0) }

        val result = service.register(validRequest("DE123"))

        assertEquals(MerchantOnboardingStatus.ACTIVE, result.status)
        assertTrue(result.applicationReference.startsWith("BLTZ-"))
        assertEquals("Acme GmbH", result.businessProfile.legalBusinessName)
        verify(repository).save(any())
        verify(auditTrail).record(any())
        verify(observabilitySupport).recordSuccess("register", MerchantOnboardingStatus.ACTIVE)
    }

    @Test
    fun `register rejects duplicate active registration number`() {
        whenever(
            repository.existsByBusinessProfileRegistrationNumberAndStatusIn(eq("DE-DUP"), any())
        ).thenReturn(true)

        val ex = assertFailsWith<IllegalArgumentException> {
            service.register(validRequest("DE-DUP"))
        }
        assertTrue(ex.message!!.contains("DE-DUP"))
    }

    @Test
    fun `findById returns existing merchant`() {
        val id = UUID.randomUUID()
        val application = minimalApplication(id)
        whenever(repository.findById(id)).thenReturn(Optional.of(application))

        val result = service.findById(id)

        assertEquals(id, result.id)
    }

    @Test
    fun `findById throws NoSuchElementException when merchant not found`() {
        val id = UUID.randomUUID()
        whenever(repository.findById(id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> {
            service.findById(id)
        }
    }

    private fun validRequest(registrationNumber: String) = RegisterMerchantRequest(
        businessProfile = MerchantBusinessProfileRequest(
            legalBusinessName = "Acme GmbH",
            businessType = "LLC",
            registrationNumber = registrationNumber,
            operatingCountry = "DE",
            primaryBusinessAddress = "Hauptstraße 1, Berlin"
        ),
        primaryContact = MerchantPrimaryContactRequest(
            fullName = "Jane Doe",
            email = "jane@acme.de",
            phoneNumber = "+49123456"
        )
    )

    private fun minimalApplication(id: UUID) = MerchantApplication(
        id = id,
        applicationReference = "BLTZ-TEST01",
        businessProfile = BusinessProfile(
            legalBusinessName = "Test Corp",
            businessType = "LLC",
            registrationNumber = "TEST001",
            operatingCountry = "DE",
            primaryBusinessAddress = "Test Str. 1, Berlin"
        ),
        primaryContact = PrimaryContact(
            fullName = "Jane Doe",
            email = "jane@test.de",
            phoneNumber = "+49123"
        ),
        status = MerchantOnboardingStatus.ACTIVE,
        submittedAt = Instant.now()
    )
}
