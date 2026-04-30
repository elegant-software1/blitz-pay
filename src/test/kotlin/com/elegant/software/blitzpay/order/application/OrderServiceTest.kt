package com.elegant.software.blitzpay.order.application

import com.elegant.software.blitzpay.merchant.api.MerchantGateway
import com.elegant.software.blitzpay.merchant.api.OrderableMerchantProduct
import com.elegant.software.blitzpay.order.OrderFixtureLoader
import com.elegant.software.blitzpay.order.domain.Order
import com.elegant.software.blitzpay.order.domain.OrderItem
import com.elegant.software.blitzpay.order.repository.OrderItemRepository
import com.elegant.software.blitzpay.order.repository.OrderRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderServiceTest {
    private val merchantGateway = mock<MerchantGateway>()
    private val orderRepository = mock<OrderRepository>()
    private val orderItemRepository = mock<OrderItemRepository>()
    private val service = OrderService(merchantGateway, orderRepository, orderItemRepository)

    @Test
    fun `create persists order snapshots and totals`() {
        val request = OrderFixtureLoader.createOrderRequest()
        val merchantId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val branchId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        whenever(merchantGateway.findOrderableProducts(any())).thenReturn(
            listOf(
                OrderableMerchantProduct(request.items[0].productId, merchantId, branchId, "Coffee", "Medium roast", BigDecimal("12.50"), true),
                OrderableMerchantProduct(request.items[1].productId, merchantId, branchId, "Bagel", "Sesame", BigDecimal("4.00"), true),
            )
        )
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.saveAll(any<List<OrderItem>>())).thenAnswer { it.arguments[0] }

        val response = service.create(request)

        assertEquals(2_900L, response.totalAmountMinor)
        assertEquals(merchantId, response.merchantId)
        assertEquals(branchId, response.branchId)
        assertEquals(2, response.items.size)
        assertEquals("Coffee", response.items.first().name)
        assertEquals(2_500L, response.items.first().lineTotalMinor)
    }

    @Test
    fun `create rejects missing products`() {
        val request = OrderFixtureLoader.createOrderRequest()
        whenever(merchantGateway.findOrderableProducts(any())).thenReturn(emptyList())

        assertFailsWith<NoSuchElementException> {
            service.create(request)
        }
    }

    @Test
    fun `create rejects inactive products`() {
        val request = OrderFixtureLoader.createOrderRequest()
        val merchantId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        whenever(merchantGateway.findOrderableProducts(any())).thenReturn(
            listOf(
                OrderableMerchantProduct(request.items[0].productId, merchantId, null, "Coffee", null, BigDecimal("12.50"), false),
                OrderableMerchantProduct(request.items[1].productId, merchantId, null, "Bagel", null, BigDecimal("4.00"), true),
            )
        )

        assertFailsWith<OrderCreationConflictException> {
            service.create(request)
        }
    }

    @Test
    fun `create rejects cross merchant products`() {
        val request = OrderFixtureLoader.createOrderRequest()
        whenever(merchantGateway.findOrderableProducts(any())).thenReturn(
            listOf(
                OrderableMerchantProduct(request.items[0].productId, UUID.randomUUID(), null, "Coffee", null, BigDecimal("12.50"), true),
                OrderableMerchantProduct(request.items[1].productId, UUID.randomUUID(), null, "Bagel", null, BigDecimal("4.00"), true),
            )
        )

        assertFailsWith<OrderCreationConflictException> {
            service.create(request)
        }
    }
}
