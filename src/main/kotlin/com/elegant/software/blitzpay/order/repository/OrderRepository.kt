package com.elegant.software.blitzpay.order.repository

import com.elegant.software.blitzpay.order.domain.Order
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByOrderId(orderId: String): Order?
}
