package com.elegant.software.blitzpay.merchant.api

import java.math.BigDecimal
import java.util.UUID

interface MerchantCredentialResolver {
    fun resolveStripe(merchantId: UUID, branchId: UUID?): StripeCredentials?
    fun resolveBraintree(merchantId: UUID, branchId: UUID?): BraintreeCredentials?

    /**
     * Resolves the effective branch ID for a payment request.
     * Priority: explicit branchId → product.merchantBranchId → null (caller returns 400)
     */
    fun resolveBranch(merchantId: UUID, branchId: UUID?, productId: UUID?): UUID?

    /** Returns the stored unit price for a product, or null if product not found. */
    fun resolveProductPrice(productId: UUID): BigDecimal?
}
