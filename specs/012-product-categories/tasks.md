# Tasks: Merchant Product Categories And Product Codes

**Input**: Design documents from `/specs/012-product-categories/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Include unit, contract, and end-to-end validation tasks because the spec defines independent test criteria and acceptance scenarios.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g. `US1`, `US2`, `US3`)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Design Alignment)

**Purpose**: Bring the feature design artifacts in sync with the clarified `productCode` behavior before implementation planning continues.

- [x] T001 Update `specs/012-product-categories/plan.md` summary, source tree, and implementation sequence to include branch-scoped `productCode`, auto-number generation, and duplicate-code upsert semantics
- [x] T002 [P] Update `specs/012-product-categories/data-model.md` to document `productCode`, branch-level uniqueness, monotonic numeric generation, and request-to-existing-product behavior for duplicate codes
- [x] T003 [P] Update `specs/012-product-categories/contracts/rest-api.md` to add `productCode` to REST and MCP contracts, including duplicate-code targeting behavior
- [x] T004 [P] Update `specs/012-product-categories/quickstart.md` to add verification steps for caller-supplied `productCode`, auto-generated codes, and duplicate-code update behavior

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema and shared product primitives required by all user stories.

**⚠️ CRITICAL**: No user story work should proceed until this phase is complete.

- [x] T005 Create `src/main/resources/db/changelog/20260429-001-product-code-support.sql` to add `product_code` to `blitzpay.merchant_products`, enforce branch-scoped uniqueness, and create an index suitable for numeric next-code lookup
- [x] T006 [P] Register `src/main/resources/db/changelog/20260429-001-product-code-support.sql` in `src/main/resources/db/changelog/db.changelog-master.yaml`
- [x] T007 [P] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantProduct.kt` with `productCategoryId` and `productCode`, plus update helpers for category assignment and code changes
- [x] T008 [P] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantProductModels.kt` so `CreateProductRequest`, `UpdateProductRequest`, and `ProductResponse` include `categoryId`, `categoryName`, and `productCode`
- [x] T009 Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/repository/MerchantProductRepository.kt` with branch-category filtering, active-product counting by category, product-code existence/lookup methods, and max-numeric-code lookup support

**Checkpoint**: The schema and shared product model support both categories and branch-scoped product codes.

---

## Phase 3: User Story 1 - Merchant Creates and Manages Product Categories (Priority: P1) 🎯 MVP

**Goal**: Merchants can create, list, rename, and delete their own product categories.

**Independent Test**: Create three categories for one merchant, list them alphabetically, rename one, delete an empty category, and confirm deletion is blocked when active products still reference a category.

### Tests for User Story 1

- [x] T010 [P] [US1] Create unit coverage in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductCategoryServiceTest.kt` for create, list, rename, duplicate handling, and delete guard behavior
- [x] T011 [P] [US1] Create contract coverage in `src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantProductCategoryContractTest.kt` for POST/GET/PUT/DELETE category endpoints and their error cases

### Implementation for User Story 1

- [x] T012 [P] [US1] Create `src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantProductCategory.kt` with merchant ownership, trimmed 100-char name validation, and rename behavior
- [x] T013 [P] [US1] Create `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantProductCategoryModels.kt` with create, rename, and response DTOs
- [x] T014 [US1] Create `src/main/kotlin/com/elegant/software/blitzpay/merchant/repository/MerchantProductCategoryRepository.kt` with merchant-scoped lookup and duplicate-detection methods
- [x] T015 [US1] Create `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductCategoryService.kt` for merchant validation, sorted listing, duplicate detection, and active-product deletion blocking
- [x] T016 [US1] Create `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductCategoryController.kt` with reactive create/list/rename/delete endpoints and 400/404/409 exception mapping
- [x] T017 [US1] Extend `src/contractTest/kotlin/com/elegant/software/blitzpay/contract/ContractVerifierBase.kt` with category service/repository mocks needed by category contract tests

**Checkpoint**: User Story 1 is independently functional and testable as the MVP increment.

---

## Phase 4: User Story 2 - Merchant Assigns Category And Product Code To A Product (Priority: P2)

**Goal**: Product APIs support optional category assignment plus optional `productCode`, with branch-scoped uniqueness, automatic numeric generation, and duplicate-code targeting of the existing branch product.

**Independent Test**: Create a category, create a product with an explicit code, create another product without a code and verify the next numeric code is assigned, filter by category, then submit a create/update request using an existing branch `productCode` and verify the existing product is updated instead of duplicating it.

### Tests for User Story 2

- [x] T018 [P] [US2] Extend `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt` for valid/invalid category assignment, auto-generated codes, caller-supplied codes, branch-scoped duplicate-code targeting, and category-filtered listing

### Implementation for User Story 2

- [x] T019 [US2] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt` to validate category ownership, generate next numeric branch code, reuse existing branch product for duplicate codes, and include category/code fields in responses
- [x] T020 [US2] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt` so create/update multipart requests accept optional `categoryId` and `productCode`, and list supports optional `categoryId`
- [x] T021 [US2] Update `src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantContractTest.kt` or add targeted product contract coverage for category fields, explicit `productCode`, auto-generated `productCode`, and duplicate-code update behavior

**Checkpoint**: Product create/update/get/list flows expose `productCode` and category data correctly, including auto-number generation and duplicate-code targeting.

---

## Phase 5: User Story 3 - MCP Server Category And Product Code Tools (Priority: P3)

**Goal**: MCP tools can manage categories and interact with product updates using optional `categoryId` and `productCode`, while still flowing through application services.

**Independent Test**: Use MCP-facing tests to verify category lookup/create/list, then call `merchant_product_update` with `categoryId` and `productCode`, including a duplicate-code case that targets the existing branch product.

### Tests for User Story 3

- [x] T022 [P] [US3] Extend `src/test/kotlin/com/elegant/software/blitzpay/merchant/mcp/MerchantProductToolsTest.kt` for category lookup/create/list tools and for `merchant_product_update` passing both `categoryId` and `productCode`

### Implementation for User Story 3

- [x] T023 [US3] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/mcp/MerchantProductTool.kt` with `category_id_by_name`, `category_id_by_name_or_create`, and `merchant_list_product_categories` using application services only
- [x] T024 [US3] Extend `src/main/kotlin/com/elegant/software/blitzpay/merchant/mcp/MerchantProductTool.kt` so `merchant_product_update` and product upsert flows accept optional `categoryId` and `productCode`, preserving inactive MCP product behavior

**Checkpoint**: MCP tools support category workflows and product-code-aware updates without direct repository injection.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate the full feature and close documentation/runtime gaps.

- [x] T025 [P] Refresh any generated or handwritten API references impacted by the feature in `api-docs/api-doc.yml` and related documentation files if schema/output examples changed
- [x] T026 Run `./gradlew test --tests com.elegant.software.blitzpay.merchant.application.MerchantProductCategoryServiceTest --tests com.elegant.software.blitzpay.merchant.application.MerchantProductServiceTest --tests com.elegant.software.blitzpay.merchant.mcp.MerchantProductToolsTest contractTest --tests com.elegant.software.blitzpay.merchant.MerchantProductCategoryContractTest` and resolve failures
- [x] T027 Run `./gradlew check` from the repo root and resolve any remaining compilation, test, or Modulith verification issues
- [x] T028 [P] Execute the scenarios in `specs/012-product-categories/quickstart.md`, including explicit-code create, auto-code create, category filter, duplicate-code targeting, and blocked category delete

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; aligns the design artifacts.
- **Foundational (Phase 2)**: Depends on Phase 1; blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and benefits from User Story 1 category service/repository work.
- **User Story 3 (Phase 5)**: Depends on User Story 1 category services and User Story 2 product-code/category-aware product service behavior.
- **Polish (Phase 6)**: Depends on all user stories being complete.

### User Story Dependencies

- **US1 (P1)**: First deliverable and MVP.
- **US2 (P2)**: Requires the foundational product code/schema work and category repository/service support.
- **US3 (P3)**: Requires the category and product flows from US1 and US2.

### Within Each User Story

- Write or update tests before finalizing implementation.
- Domain/API/repository tasks precede service changes.
- Service changes precede controller or MCP adapter changes.
- Contract validation follows endpoint wiring.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel after T001.
- T007 and T008 can run in parallel once the migration shape is settled.
- T010 and T011 can run in parallel within US1.
- T012 and T013 can run in parallel within US1.
- T022 can run while T023/T024 are being implemented, as long as request/response expectations are agreed.
- T025 and T028 can run in parallel after the full implementation is stable.

---

## Parallel Example: User Story 1

```bash
# Tests in parallel
T010  src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductCategoryServiceTest.kt
T011  src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantProductCategoryContractTest.kt

# Model + DTO work in parallel
T012  src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantProductCategory.kt
T013  src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantProductCategoryModels.kt
```

## Parallel Example: User Story 2

```bash
# Product service test and controller adaptation can overlap once request/response shapes are fixed
T018  src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt
T020  src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (User Story 1).
3. Validate category CRUD independently.
4. Demo/deploy category management before moving to product code behavior.

### Incremental Delivery

1. Ship category CRUD (US1).
2. Add product category assignment and `productCode` behavior (US2).
3. Add MCP support for category and `productCode` workflows (US3).
4. Finish with full validation and documentation refresh.

### Suggested MVP Scope

- **MVP**: Phase 1, Phase 2, and Phase 3 (User Story 1 only)
- **Next Increment**: User Story 2 for branch-scoped `productCode` and category assignment
- **Final Increment**: User Story 3 plus polish

---

## Notes

- Every task follows the required checklist format with task ID and file path.
- Tests are included because the spec explicitly defines independent verification scenarios.
- MCP tasks keep the service-only boundary already established in this repository.
- `productCode` behavior now drives both schema and service logic, so the foundational phase includes it explicitly.
