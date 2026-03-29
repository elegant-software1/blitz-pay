package com.elegant.software.blitzpay.merchant.web

import com.elegant.software.blitzpay.merchant.api.BranchResponse
import com.elegant.software.blitzpay.merchant.api.CreateBranchRequest
import com.elegant.software.blitzpay.merchant.application.MerchantBranchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class BranchErrorResponse(val error: String)

@Tag(name = "Merchant Branches", description = "Manage branches under a merchant")
@RestController
@RequestMapping("/{version:v\\d+(?:\\.\\d+)*}/merchants/{merchantId}/branches", version = "1")
class MerchantBranchController(private val merchantBranchService: MerchantBranchService) {

    @Operation(summary = "Create a new branch under a merchant")
    @PostMapping
    fun create(
        @PathVariable merchantId: UUID,
        @RequestBody request: CreateBranchRequest,
    ): ResponseEntity<Any> {
        if (request.name.isBlank()) {
            return ResponseEntity.badRequest().body(BranchErrorResponse("name must not be blank"))
        }
        return try {
            val response = merchantBranchService.create(merchantId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(BranchErrorResponse(ex.message ?: "not found"))
        }
    }

    @Operation(summary = "List active branches for a merchant")
    @GetMapping
    fun list(@PathVariable merchantId: UUID): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(merchantBranchService.list(merchantId))
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(BranchErrorResponse(ex.message ?: "not found"))
        }
    }

    @Operation(summary = "Get a branch by ID")
    @GetMapping("/{branchId}")
    fun get(
        @PathVariable merchantId: UUID,
        @PathVariable branchId: UUID,
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(merchantBranchService.get(merchantId, branchId))
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(BranchErrorResponse(ex.message ?: "not found"))
        }
    }
}
