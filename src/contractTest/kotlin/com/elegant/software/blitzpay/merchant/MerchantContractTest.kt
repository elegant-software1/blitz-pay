package com.elegant.software.blitzpay.merchant

import com.elegant.software.blitzpay.contract.ContractVerifierBase
import com.elegant.software.blitzpay.merchant.domain.BusinessProfile
import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import com.elegant.software.blitzpay.merchant.domain.MerchantProduct
import com.elegant.software.blitzpay.merchant.domain.PrimaryContact
import jakarta.persistence.Query
import org.hibernate.Filter
import org.hibernate.Session
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import java.util.Optional
import java.util.UUID
import java.math.BigDecimal

class MerchantContractTest : ContractVerifierBase() {
    private val session = mock<Session>()
    private val hibernateFilter = mock<Filter>()
    private val nativeQuery = mock<Query>()

    @BeforeEach
    fun setupMerchantContracts() {
        whenever(entityManager.unwrap(Session::class.java)).thenReturn(session)
        whenever(session.enableFilter(any())).thenReturn(hibernateFilter)
        whenever(hibernateFilter.setParameter(any<String>(), any())).thenReturn(hibernateFilter)
        whenever(entityManager.createNativeQuery(any<String>())).thenReturn(nativeQuery)
        whenever(nativeQuery.setParameter(any<String>(), any())).thenReturn(nativeQuery)
        whenever(nativeQuery.singleResult).thenReturn("contract")
        whenever(storageService.presignDownload(any(), any())).thenAnswer { "https://signed.example/${it.arguments[0]}" }
    }

    @Test
    fun `POST merchants returns 201 and ACTIVE status for valid registration`() {
        whenever(merchantApplicationRepository.existsByBusinessProfileRegistrationNumberAndStatusIn(any(), any()))
            .thenReturn(false)
        whenever(merchantApplicationRepository.save(any<MerchantApplication>()))
            .thenAnswer { it.arguments[0] }

        webTestClient.post()
            .uri("/v1/merchants")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "legalBusinessName": "Acme GmbH",
                  "businessType": "LLC",
                  "registrationNumber": "DE123456789",
                  "operatingCountry": "DE",
                  "primaryBusinessAddress": "Hauptstrasse 1, 10115 Berlin",
                  "contactFullName": "Jane Doe",
                  "contactEmail": "jane@acme.de",
                  "contactPhoneNumber": "+4930123456"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.status").isEqualTo(MerchantOnboardingStatus.ACTIVE.name)
            .jsonPath("$.applicationId").exists()
            .jsonPath("$.applicationReference").value<String> { ref ->
                require(ref.startsWith("BLTZ-")) { "Expected BLTZ- prefix, got $ref" }
            }
    }

    @Test
    fun `POST merchants returns 409 when registration number already active`() {
        whenever(merchantApplicationRepository.existsByBusinessProfileRegistrationNumberAndStatusIn(any(), any()))
            .thenReturn(true)

        webTestClient.post()
            .uri("/v1/merchants")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "legalBusinessName": "Duplicate GmbH",
                  "businessType": "LLC",
                  "registrationNumber": "DE-DUPLICATE",
                  "operatingCountry": "DE",
                  "primaryBusinessAddress": "Somestrasse 1",
                  "contactFullName": "John Smith",
                  "contactEmail": "john@dup.de",
                  "contactPhoneNumber": "+4930000000"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    fun `GET merchant by id returns 200 when found`() {
        val application = MerchantApplication(
            applicationReference = "BLTZ-CONTRACT1",
            businessProfile = BusinessProfile(
                legalBusinessName = "Test GmbH",
                businessType = "LLC",
                registrationNumber = "DE-CONTRACT-001",
                operatingCountry = "DE",
                primaryBusinessAddress = "Teststrasse 1, Berlin"
            ),
            primaryContact = PrimaryContact(fullName = "Test User", email = "test@test.de", phoneNumber = "+49301234567")
        )
        whenever(merchantApplicationRepository.findById(application.id))
            .thenReturn(Optional.of(application))

        webTestClient.get()
            .uri("/v1/merchants/${application.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.applicationId").isEqualTo(application.id.toString())
    }

    @Test
    fun `GET merchant by id returns 404 when not found`() {
        val unknownId = UUID.randomUUID()
        whenever(merchantApplicationRepository.findById(unknownId))
            .thenReturn(Optional.empty())

        webTestClient.get()
            .uri("/v1/merchants/$unknownId")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `POST product accepts multipart product fields and image`() {
        val merchantId = UUID.randomUUID()
        whenever(merchantApplicationRepository.existsById(merchantId)).thenReturn(true)
        whenever(merchantProductRepository.save(any<MerchantProduct>())).thenAnswer { it.arguments[0] }
        val multipart = MultipartBodyBuilder().apply {
            part("name", "Coffee Blend")
            part("description", "**Medium roast**")
            part("unitPrice", "12.50")
            part("image", byteArrayOf(1, 2, 3))
                .filename("coffee.webp")
                .contentType(MediaType("image", "webp"))
        }

        webTestClient.post()
            .uri("/v1/merchants/$merchantId/products")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipart.build())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.name").isEqualTo("Coffee Blend")
            .jsonPath("$.description").isEqualTo("**Medium roast**")
            .jsonPath("$.imageUrl").exists()
    }

    @Test
    fun `GET products returns signed image url and description`() {
        val merchantId = UUID.randomUUID()
        whenever(merchantApplicationRepository.existsById(merchantId)).thenReturn(true)
        whenever(merchantProductRepository.findAllByActiveTrue()).thenReturn(
            listOf(
                MerchantProduct(
                    merchantApplicationId = merchantId,
                    name = "Coffee Blend",
                    description = "**Medium roast**",
                    unitPrice = BigDecimal("12.50"),
                    imageStorageKey = "merchants/$merchantId/products/product/image.webp"
                )
            )
        )

        webTestClient.get()
            .uri("/v1/merchants/$merchantId/products")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.products[0].description").isEqualTo("**Medium roast**")
            .jsonPath("$.products[0].imageUrl").isEqualTo("https://signed.example/merchants/$merchantId/products/product/image.webp")
    }

    @Test
    fun `POST product rejects unsupported image type`() {
        val merchantId = UUID.randomUUID()
        whenever(merchantApplicationRepository.existsById(merchantId)).thenReturn(true)
        val multipart = MultipartBodyBuilder().apply {
            part("name", "Coffee Blend")
            part("unitPrice", "12.50")
            part("image", "not an image".toByteArray())
                .filename("notes.txt")
                .contentType(MediaType.TEXT_PLAIN)
        }

        webTestClient.post()
            .uri("/v1/merchants/$merchantId/products")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipart.build())
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").exists()
    }
}
