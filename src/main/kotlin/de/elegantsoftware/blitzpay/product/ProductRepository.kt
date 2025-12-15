package de.elegantsoftware.blitzpay.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ProductRepository : JpaRepository<Product, Long> {

    // Add this method for fetching products with their merchants
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.merchant WHERE p.id IN :ids")
    fun findAllByIdWithMerchant(ids: Collection<Long>): List<Product>

    // Optional: You might also want a method for a single product
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.merchant WHERE p.id = :id")
    fun findByIdWithMerchant(id: Long): Optional<Product>
}