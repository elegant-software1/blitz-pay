package com.elegant.software.blitzpay.order.application

import com.elegant.software.blitzpay.merchant.api.MerchantGateway
import com.elegant.software.blitzpay.order.api.CreateOrderRequest
import com.elegant.software.blitzpay.order.api.OrderResponse
import com.elegant.software.blitzpay.order.api.toResponse
import com.elegant.software.blitzpay.order.domain.Order
import com.elegant.software.blitzpay.order.domain.OrderItem
import com.elegant.software.blitzpay.order.domain.unitPriceMinor
import com.elegant.software.blitzpay.order.repository.OrderItemRepository
import com.elegant.software.blitzpay.order.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderService(
    private val merchantGateway: MerchantGateway,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    fun create(request: CreateOrderRequest): OrderResponse {
        require(request.items.isNotEmpty()) { "Order must contain at least one item" }
        request.items.forEach { require(it.quantity > 0) { "quantity must be > 0 for product ${it.productId}" } }

        val products = merchantGateway.findOrderableProducts(request.items.map { it.productId }.distinct())
        val productsById = products.associateBy { it.productId }
        val missingProducts = request.items.map { it.productId }.distinct().filterNot(productsById::containsKey)
        if (missingProducts.isNotEmpty()) {
            throw NoSuchElementException("Products not found: ${missingProducts.joinToString(",")}")
        }

        val inactiveProducts = products.filterNot { it.active }.map { it.productId }
        if (inactiveProducts.isNotEmpty()) {
            throw OrderCreationConflictException("Products are not orderable: ${inactiveProducts.joinToString(",")}")
        }

        val merchantIds = products.map { it.merchantApplicationId }.distinct()
        if (merchantIds.size != 1) {
            throw OrderCreationConflictException("All ordered products must belong to the same merchant")
        }

        val order = Order(
            orderId = Order.nextOrderId(),
            merchantApplicationId = merchantIds.single(),
            merchantBranchId = products.mapNotNull { it.branchId }.distinct().singleOrNull(),
            currency = DEFAULT_CURRENCY,
            totalAmountMinor = request.items.sumOf { productsById.getValue(it.productId).unitPriceMinor() * it.quantity },
            itemCount = request.items.sumOf { it.quantity },
        )
        val savedOrder = orderRepository.save(order)
        val savedItems = orderItemRepository.saveAll(
            request.items.map {
                OrderItem.fromProduct(savedOrder.id, productsById.getValue(it.productId), it.quantity)
            }
        )
        return savedOrder.toResponse(savedItems)
    }

    @Transactional(readOnly = true)
    fun get(orderId: String): OrderResponse {
        val order = orderRepository.findByOrderId(orderId)
            ?: throw NoSuchElementException("Order not found: $orderId")
        val items = orderItemRepository.findAllByOrderIdFk(order.id)
        return order.toResponse(items)
    }

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
    }
}

class OrderCreationConflictException(message: String) : IllegalStateException(message)
