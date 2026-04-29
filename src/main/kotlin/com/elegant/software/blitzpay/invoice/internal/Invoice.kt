package com.elegant.software.blitzpay.invoice.internal

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Persistable

@Entity
@Table(name = "invoices")
class Invoice(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private val id: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    var paymentStatus: PaymentStatus,
) : Persistable<UUID> {

    @Transient
    private var isNew: Boolean = true

    @OneToMany(
        mappedBy = "invoice",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private val recipientEntities: MutableList<InvoiceRecipient> = mutableListOf()

    init {
        require(amount >= BigDecimal.ZERO) { "amount must be non-negative" }
    }

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNew

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNew = false
    }

    val recipients: List<InvoiceRecipient>
        get() = recipientEntities.toList()

    fun addRecipient(recipient: InvoiceRecipient) {
        recipient.attachTo(this)
        recipientEntities.add(recipient)
    }

    fun addRecipients(recipients: Iterable<InvoiceRecipient>) {
        recipients.forEach(::addRecipient)
    }

    @PrePersist
    fun validateBeforePersist() {
        require(recipientEntities.isNotEmpty()) { "invoice must contain at least one recipient" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invoice) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
