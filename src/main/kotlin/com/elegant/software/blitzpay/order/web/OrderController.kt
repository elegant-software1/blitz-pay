package com.elegant.software.blitzpay.order.web

import com.elegant.software.blitzpay.order.api.CreateOrderRequest
import com.elegant.software.blitzpay.order.api.OrderResponse
import com.elegant.software.blitzpay.order.application.OrderCreationConflictException
import com.elegant.software.blitzpay.order.application.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

data class OrderErrorResponse(val error: String)

@Tag(name = "Orders", description = "Create and inspect order payment lifecycle records")
@RestController
@RequestMapping("/{version:v\\d+(?:\\.\\d+)*}/orders", version = "1")
class OrderController(
    private val orderService: OrderService,
) {
    @Operation(summary = "Create an order from merchant products")
    @PostMapping
    fun create(@RequestBody request: CreateOrderRequest): Mono<ResponseEntity<OrderResponse>> =
        Mono.fromCallable { orderService.create(request) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @Operation(summary = "Get the latest business status for an order")
    @GetMapping("/{orderId}")
    fun get(@PathVariable orderId: String): Mono<ResponseEntity<OrderResponse>> =
        Mono.fromCallable { orderService.get(orderId) }
            .subscribeOn(Schedulers.boundedElastic())
            .map { ResponseEntity.ok(it) }

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(ex: IllegalArgumentException): ResponseEntity<OrderErrorResponse> =
        ResponseEntity.badRequest().body(OrderErrorResponse(ex.message ?: "Invalid order request"))

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(ex: NoSuchElementException): ResponseEntity<OrderErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(OrderErrorResponse(ex.message ?: "Order not found"))

    @ExceptionHandler(OrderCreationConflictException::class)
    fun unprocessable(ex: OrderCreationConflictException): ResponseEntity<OrderErrorResponse> =
        ResponseEntity.unprocessableEntity().body(OrderErrorResponse(ex.message ?: "Order cannot be created"))
}
