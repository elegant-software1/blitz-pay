package com.elegant.software.blitzpay.invoice.internal

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

internal interface InvoiceRepository : JpaRepository<Invoice, UUID> {

    @EntityGraph(attributePaths = ["recipientEntities"])
    override fun findById(id: UUID): Optional<Invoice>
}
