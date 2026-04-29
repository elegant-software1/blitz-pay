# Quickstart: Merchant Product Categories

**Feature**: 012-product-categories  
**Date**: 2026-04-28

## Prerequisites

- Working local environment (see root README)
- Liquibase migrations applied (`./gradlew bootRun` runs them on startup)
- A registered merchant with at least one branch and product

## What's Being Added

1. **New table** `blitzpay.merchant_product_categories` — per-merchant category catalogue
2. **FK column** `product_category_id` on `blitzpay.merchant_products` (nullable)
3. **Product code column** `product_code` on `blitzpay.merchant_products` (branch-scoped unique numeric code)
4. **REST API** — 4 new endpoints under `/{version}/merchants/{merchantId}/product-categories`
5. **Product endpoints** — category field plus `productCode` on create/update/list/get
6. **MCP tools** — 3 new tools + `merchant_product_update` extended with `categoryId` and `productCode`

## Files to Create

| File | Purpose |
|------|---------|
| `src/main/resources/db/changelog/20260428-003-merchant-product-categories.sql` | DDL for new table + RLS |
| `src/main/resources/db/changelog/20260428-004-merchant-products-add-category-fk.sql` | Adds FK column to products |
| `src/main/resources/db/changelog/20260429-001-product-code-support.sql` | Adds branch-scoped product code support |
| `src/main/kotlin/.../merchant/domain/MerchantProductCategory.kt` | JPA entity |
| `src/main/kotlin/.../merchant/repository/MerchantProductCategoryRepository.kt` | Spring Data repo |
| `src/main/kotlin/.../merchant/api/MerchantProductCategoryModels.kt` | Request/response models |
| `src/main/kotlin/.../merchant/application/MerchantProductCategoryService.kt` | Business logic |
| `src/main/kotlin/.../merchant/web/MerchantProductCategoryController.kt` | REST controller |
| `src/test/kotlin/.../merchant/application/MerchantProductCategoryServiceTest.kt` | Unit tests |
| `src/contractTest/kotlin/.../merchant/MerchantProductCategoryContractTest.kt` | Contract tests |

## Files to Modify

| File | Change |
|------|--------|
| `domain/MerchantProduct.kt` | Add `productCategoryId: UUID?` and `productCode: Long?`; extend `update()` |
| `repository/MerchantProductRepository.kt` | Add category filter plus branch `productCode` lookup/max-code queries |
| `api/MerchantProductModels.kt` | Add `categoryId`/`categoryName`/`productCode` to responses and requests |
| `application/MerchantProductService.kt` | Pass category through, generate next branch `productCode`, and route duplicate codes to the existing branch product |
| `web/MerchantProductController.kt` | Accept optional `categoryId` and `productCode` form parts on create/update; optional `?categoryId` on list |
| `mcp/MerchantProductTool.kt` | Add category tools; extend product update/upsert flows with `categoryId` and `productCode` |

## Running

```bash
# Build and test
./gradlew check

# Run a specific test
./gradlew test --tests "com.elegant.software.blitzpay.merchant.application.MerchantProductCategoryServiceTest"

# Run contract tests
./gradlew contractTest
```

## Verifying the Feature

1. Create a category:
   ```
   POST /v1/merchants/{merchantId}/product-categories
   {"name": "Drinks"}
   → 201 with id
   ```
2. Create a product with that category:
   ```
   POST /v1/merchants/{merchantId}/products  (multipart)
   categoryId=<id from step 1>
   productCode=12
   → 201 with categoryId + categoryName + productCode populated
   ```
3. Create another product without `productCode`:
   ```
   POST /v1/merchants/{merchantId}/products  (multipart)
   categoryId=<id from step 1>
   → 201 with productCode auto-generated as the next highest branch value
   ```
4. List products filtered by category:
   ```
   GET /v1/merchants/{merchantId}/products?branchId=...&categoryId=<id>
   → only products in "Drinks"
   ```
5. Re-submit a create or update request with an existing branch `productCode`:
   ```
   POST or PUT /v1/merchants/{merchantId}/products
   productCode=12
   → existing branch product with code 12 is updated instead of duplicating
   ```
6. Try to delete a category that has a product:
   ```
   DELETE /v1/merchants/{merchantId}/product-categories/{id}
   → 409 Conflict
   ```
