package com.elegant.software.blitzpay.order.api

import com.elegant.software.blitzpay.order.domain.Order
import com.elegant.software.blitzpay.order.domain.OrderItem
import com.elegant.software.blitzpay.order.domain.OrderStatus
import java.time.Instant
import java.util.UUID

data class CreateOrderRequest(
    val items: List<CreateOrderItemRequest> = emptyList(),
)

data class CreateOrderItemRequest(
    val productId: UUID,
    val quantity: Int,
)

data class OrderItemResponse(
    val productId: UUID,
    val name: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val lineTotalMinor: Long,
)

data class OrderResponse(
    val orderId: String,
    val merchantId: UUID,
    val branchId: UUID?,
    val status: OrderStatus,
    val currency: String,
    val totalAmountMinor: Long,
    val items: List<OrderItemResponse>,
    val createdAt: Instant,
    val lastPaymentRequestId: UUID? = null,
    val lastPaymentProvider: String? = null,
    val paidAt: Instant? = null,
)

internal fun Order.toResponse(items: List<OrderItem>) = OrderResponse(
    orderId = orderId,
    merchantId = merchantApplicationId,
    branchId = merchantBranchId,
    status = status,
    currency = currency,
    totalAmountMinor = totalAmountMinor,
    items = items.sortedBy { it.createdAt }.map {
        OrderItemResponse(
            productId = it.merchantProductId,
            name = it.productName,
            quantity = it.quantity,
            unitPriceMinor = it.unitPriceMinor,
            lineTotalMinor = it.lineTotalMinor,
        )
    },
    createdAt = createdAt,
    lastPaymentRequestId = lastPaymentRequestId,
    lastPaymentProvider = lastPaymentProvider,
    paidAt = paidAt,
)
