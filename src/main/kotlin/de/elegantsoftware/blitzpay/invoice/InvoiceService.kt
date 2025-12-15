package de.elegantsoftware.blitzpay.invoice

import de.elegantsoftware.blitzpay.merchant.MerchantRepository
import de.elegantsoftware.blitzpay.product.Product
import de.elegantsoftware.blitzpay.product.ProductRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service

data class InvoiceItemRequest(
    val productId: Long,
    val quantity: Int
)

data class CreateInvoiceRequest(
    val merchantId: Long,
    val items: List<InvoiceItemRequest>
)

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceItemRepository: InvoiceItemRepository,
    private val merchantRepository: MerchantRepository,
    private val productRepository: ProductRepository
) {

//    fun create(request: CreateInvoiceRequest): Invoice {
//        val merchant = merchantRepository.findById(request.merchantId).orElseThrow()
//
//        val invoice = invoiceRepository.save(
//            Invoice(
//                merchant = merchant
//            )
//        )
//
//        val invoiceItems = request.items.map { item ->
//            val product = productRepository.findById(item.productId).orElseThrow()
//
//            InvoiceItem(
//                quantity = item.quantity,
//                product = product,
//                invoice = invoice
//            )
//        }
//
//        invoiceItemRepository.saveAll(invoiceItems)
//
//        return invoice.copy(items = invoiceItems)
//    }
fun create(request: CreateInvoiceRequest): Invoice {
    val merchant = merchantRepository.findById(request.merchantId).orElseThrow()

    val invoice = invoiceRepository.save(
        Invoice(
            merchant = merchant
        )
    )

    // Fetch all products with their merchants in one query
    val productIds = request.items.map { it.productId }
    val products = productRepository.findAllByIdWithMerchant(productIds)
        .associateBy(Product::productId)

    val invoiceItems = request.items.map { item ->
        val product = products[item.productId] ?:
        throw EntityNotFoundException("Product not found: ${item.productId}")

        InvoiceItem(
            quantity = item.quantity,
            product = product,
            invoice = invoice
        )
    }

    invoiceItemRepository.saveAll(invoiceItems)

    return invoice.copy(items = invoiceItems)
}
    fun findAll(): List<Invoice> = invoiceRepository.findAll()

    fun findById(id: Long): Invoice = invoiceRepository.findById(id).orElseThrow()

    fun delete(id: Long) = invoiceRepository.deleteById(id)
}
