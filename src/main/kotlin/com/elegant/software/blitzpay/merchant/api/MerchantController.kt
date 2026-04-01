package com.elegant.software.blitzpay.merchant.api

import com.elegant.software.blitzpay.merchant.application.MerchantRegistrationService
import com.elegant.software.blitzpay.merchant.domain.MerchantApplication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@RestController
@RequestMapping("/v1/merchants")
class MerchantController(
    private val merchantRegistrationService: MerchantRegistrationService
) {

    @PostMapping
    fun register(@RequestBody request: RegisterMerchantRequest): Mono<ResponseEntity<MerchantApplicationResponse>> =
        Mono.fromCallable { merchantRegistrationService.register(request) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it.toResponse()) }
            .onErrorMap(IllegalArgumentException::class.java) { ex ->
                if (ex.message?.contains("already exists") == true) {
                    ResponseStatusException(HttpStatus.CONFLICT, ex.message)
                } else {
                    ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
                }
            }
            .onErrorMap(NoSuchElementException::class.java) { ex ->
                ResponseStatusException(HttpStatus.NOT_FOUND, ex.message)
            }

    @GetMapping("/{merchantId}")
    fun get(@PathVariable merchantId: UUID): Mono<ResponseEntity<MerchantApplicationResponse>> =
        Mono.fromCallable { merchantRegistrationService.findById(merchantId) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.ok(it.toResponse()) }
            .onErrorMap(NoSuchElementException::class.java) { ex ->
                ResponseStatusException(HttpStatus.NOT_FOUND, ex.message)
            }

    private fun MerchantApplication.toResponse() = MerchantApplicationResponse(
        applicationId = id,
        applicationReference = applicationReference,
        status = status,
        businessProfile = MerchantBusinessProfileResponse(
            legalBusinessName = businessProfile.legalBusinessName,
            businessType = businessProfile.businessType,
            registrationNumber = businessProfile.registrationNumber,
            operatingCountry = businessProfile.operatingCountry,
            primaryBusinessAddress = businessProfile.primaryBusinessAddress
        ),
        primaryContact = MerchantPrimaryContactResponse(
            fullName = primaryContact.fullName,
            email = primaryContact.email,
            phoneNumber = primaryContact.phoneNumber
        ),
        people = people.map { person ->
            MerchantPersonResponse(
                fullName = person.fullName,
                role = person.role,
                countryOfResidence = person.countryOfResidence,
                ownershipPercentage = person.ownershipPercentage
            )
        },
        supportingMaterials = supportingMaterials.map { material ->
            MerchantSupportingMaterialResponse(
                type = material.type,
                fileName = material.fileName,
                uploadedAt = material.uploadedAt
            )
        },
        submittedAt = submittedAt,
        lastUpdatedAt = lastUpdatedAt
    )
}
