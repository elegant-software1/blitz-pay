package com.elegant.software.blitzpay.merchant.repository

import com.elegant.software.blitzpay.merchant.domain.MerchantProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import java.util.UUID

interface MerchantProductRepository : JpaRepository<MerchantProduct, UUID> {
    fun findByNameAndMerchantApplicationIdAndMerchantBranchIdAndActiveTrue(
        name: String,
        merchantApplicationId: UUID,
        merchantBranchId: UUID
    ): MerchantProduct?

    fun findByNameAndMerchantApplicationIdAndMerchantBranchId(
        name: String,
        merchantApplicationId: UUID,
        merchantBranchId: UUID
    ): MerchantProduct?

    @Modifying
    @Transactional
    @Query("UPDATE MerchantProduct p SET p.status = :status WHERE p.id = :id")
    fun updateStatus(@Param("id") id: UUID, @Param("status") status: String): Int

    fun findAllByActiveTrue(): List<MerchantProduct>
    fun findAllByActiveTrueAndMerchantBranchId(merchantBranchId: UUID): List<MerchantProduct>
    fun findByIdAndActiveTrue(id: UUID): Optional<MerchantProduct>
    fun findByIdAndActiveTrueAndMerchantBranchId(id: UUID, merchantBranchId: UUID): Optional<MerchantProduct>
}
