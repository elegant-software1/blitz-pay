package com.elegant.software.blitzpay.merchant

import com.elegant.software.blitzpay.contract.ContractVerifierBase
import com.elegant.software.blitzpay.merchant.application.MerchantRegistrationService
import com.elegant.software.blitzpay.merchant.domain.BusinessProfile
import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import com.elegant.software.blitzpay.merchant.domain.PrimaryContact
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.UUID

class MerchantControllerTest : ContractVerifierBase() {

    @MockitoBean
    private lateinit var merchantRegistrationService: MerchantRegistrationService

    @Test
    fun `POST v1 merchants registers merchant and returns 201 with ACTIVE status`() {
        val fixedId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")
        val activatedAt = Instant.parse("2026-03-29T10:00:00Z")
        whenever(merchantRegistrationService.register(any())).thenReturn(
            activeApplication(fixedId, "DE123456789", activatedAt)
        )

        webTestClient.post()
            .uri("/v1/merchants")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "businessProfile": {
                    "legalBusinessName": "Acme GmbH",
                    "businessType": "LLC",
                    "registrationNumber": "DE123456789",
                    "operatingCountry": "DE",
                    "primaryBusinessAddress": "Hauptstraße 1, 10115 Berlin, Germany"
                  },
                  "primaryContact": {
                    "fullName": "Jane Doe",
                    "email": "jane.doe@acme.de",
                    "phoneNumber": "+49301234567"
                  }
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.applicationId").isEqualTo(fixedId.toString())
            .jsonPath("$.applicationReference").value<String> { ref ->
                require(ref.startsWith("BLTZ-")) { "applicationReference must start with BLTZ- but was '$ref'" }
            }
            .jsonPath("$.status").isEqualTo("ACTIVE")
            .jsonPath("$.businessProfile.legalBusinessName").isEqualTo("Acme GmbH")
            .jsonPath("$.businessProfile.registrationNumber").isEqualTo("DE123456789")
            .jsonPath("$.primaryContact.email").isEqualTo("jane.doe@acme.de")
            .jsonPath("$.submittedAt").isNotEmpty
    }

    @Test
    fun `POST v1 merchants returns 409 when registration number already exists`() {
        whenever(merchantRegistrationService.register(any())).thenThrow(
            IllegalArgumentException("An active merchant application already exists for registration number DE123456789")
        )

        webTestClient.post()
            .uri("/v1/merchants")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "businessProfile": {
                    "legalBusinessName": "Acme GmbH",
                    "businessType": "LLC",
                    "registrationNumber": "DE123456789",
                    "operatingCountry": "DE",
                    "primaryBusinessAddress": "Hauptstraße 1, 10115 Berlin, Germany"
                  },
                  "primaryContact": {
                    "fullName": "Jane Doe",
                    "email": "jane.doe@acme.de",
                    "phoneNumber": "+49301234567"
                  }
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    fun `GET v1 merchants returns registered merchant`() {
        val fixedId = UUID.fromString("aaaabbbb-5717-4562-b3fc-2c963f66afa6")
        val activatedAt = Instant.parse("2026-03-29T10:00:00Z")
        whenever(merchantRegistrationService.findById(fixedId)).thenReturn(
            activeApplication(fixedId, "GB-FETCH-001", activatedAt)
        )

        webTestClient.get()
            .uri("/v1/merchants/$fixedId")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.applicationId").isEqualTo(fixedId.toString())
            .jsonPath("$.status").isEqualTo("ACTIVE")
            .jsonPath("$.businessProfile.registrationNumber").isEqualTo("GB-FETCH-001")
    }

    @Test
    fun `GET v1 merchants with unknown id returns 404`() {
        val unknownId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        whenever(merchantRegistrationService.findById(unknownId)).thenThrow(
            NoSuchElementException("Merchant not found: $unknownId")
        )

        webTestClient.get()
            .uri("/v1/merchants/$unknownId")
            .exchange()
            .expectStatus().isNotFound
    }

    private fun activeApplication(id: UUID, registrationNumber: String, activatedAt: Instant) =
        MerchantApplication(
            id = id,
            applicationReference = "BLTZ-TEST01",
            businessProfile = BusinessProfile(
                legalBusinessName = "Acme GmbH",
                businessType = "LLC",
                registrationNumber = registrationNumber,
                operatingCountry = "DE",
                primaryBusinessAddress = "Hauptstraße 1, 10115 Berlin, Germany"
            ),
            primaryContact = PrimaryContact(
                fullName = "Jane Doe",
                email = "jane.doe@acme.de",
                phoneNumber = "+49301234567"
            ),
            status = MerchantOnboardingStatus.ACTIVE,
            submittedAt = activatedAt,
            lastUpdatedAt = activatedAt
        )
}
