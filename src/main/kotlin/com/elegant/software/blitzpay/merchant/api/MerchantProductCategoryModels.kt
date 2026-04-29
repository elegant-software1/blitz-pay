package com.elegant.software.blitzpay.merchant.api

import java.time.Instant
import java.util.UUID

data class CreateProductCategoryRequest(val name: String)

data class RenameProductCategoryRequest(val name: String)

data class ProductCategoryResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
