package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.api.BraintreeCredentials
import com.elegant.software.blitzpay.merchant.api.MerchantCredentialResolver
import com.elegant.software.blitzpay.merchant.api.StripeCredentials
import java.math.BigDecimal
import com.elegant.software.blitzpay.merchant.repository.MerchantApplicationRepository
import com.elegant.software.blitzpay.merchant.repository.MerchantBranchRepository
import com.elegant.software.blitzpay.merchant.repository.MerchantProductRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MerchantCredentialResolverImpl(
    private val merchantApplicationRepository: MerchantApplicationRepository,
    private val merchantBranchRepository: MerchantBranchRepository,
    private val merchantProductRepository: MerchantProductRepository,
) : MerchantCredentialResolver {

    override fun resolveStripe(merchantId: UUID, branchId: UUID?): StripeCredentials? {
        if (branchId != null) {
            val branch = merchantBranchRepository.findByIdAndActiveTrue(branchId)
            if (branch?.stripeSecretKey != null && branch.stripePublishableKey != null) {
                return StripeCredentials(branch.stripeSecretKey!!, branch.stripePublishableKey!!)
            }
        }
        val merchant = merchantApplicationRepository.findById(merchantId).orElse(null) ?: return null
        if (merchant.stripeSecretKey != null && merchant.stripePublishableKey != null) {
            return StripeCredentials(merchant.stripeSecretKey!!, merchant.stripePublishableKey!!)
        }
        return null
    }

    override fun resolveBraintree(merchantId: UUID, branchId: UUID?): BraintreeCredentials? {
        if (branchId != null) {
            val branch = merchantBranchRepository.findByIdAndActiveTrue(branchId)
            if (branch?.braintreeMerchantId != null &&
                branch.braintreePublicKey != null &&
                branch.braintreePrivateKey != null
            ) {
                return BraintreeCredentials(
                    merchantId = branch.braintreeMerchantId!!,
                    publicKey = branch.braintreePublicKey!!,
                    privateKey = branch.braintreePrivateKey!!,
                    environment = branch.braintreeEnvironment ?: "sandbox",
                )
            }
        }
        val merchant = merchantApplicationRepository.findById(merchantId).orElse(null) ?: return null
        if (merchant.braintreeMerchantId != null &&
            merchant.braintreePublicKey != null &&
            merchant.braintreePrivateKey != null
        ) {
            return BraintreeCredentials(
                merchantId = merchant.braintreeMerchantId!!,
                publicKey = merchant.braintreePublicKey!!,
                privateKey = merchant.braintreePrivateKey!!,
                environment = merchant.braintreeEnvironment ?: "sandbox",
            )
        }
        return null
    }

    override fun resolveBranch(merchantId: UUID, branchId: UUID?, productId: UUID?): UUID? {
        if (branchId != null) {
            return branchId
        }
        if (productId != null) {
            return merchantProductRepository.findByIdAndActiveTrue(productId).orElse(null)?.merchantBranchId
        }
        return null
    }

    override fun resolveProductPrice(productId: UUID): BigDecimal? =
        merchantProductRepository.findByIdAndActiveTrue(productId).orElse(null)?.unitPrice
}
