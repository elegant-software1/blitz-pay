package com.elegant.software.blitzpay.payments.truelayer.outbound

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QrPaymentCleanupScheduler(
    private val qrPaymentService: QrPaymentService
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 300000)
    fun cleanupExpiredPayments() {
        logger.debug { "Running QR payment cleanup" }
        qrPaymentService.cleanupExpiredPayments()
    }
}
