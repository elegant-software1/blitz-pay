# Tasks: Merchant Product Image Upload API

**Input**: Design documents from `/specs/001-merchant-onboarding/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Scope decision**: This task list covers the current product catalog increment for `001-merchant-onboarding`: optional Markdown product descriptions, multipart product create/update, private S3-compatible image storage, persisted object keys, and signed retrieval URLs. It does not regenerate the broader merchant onboarding backlog.

**Tests**: Test tasks are included because `AGENTS.md`, the plan, and quickstart require tests for behavior changes.

**Organization**: Tasks are grouped by independently testable product-image user stories.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel because it touches different files or only depends on completed foundation work
- **[Story]**: User story label (`US1`, `US2`, `US3`) used only in user story phases
- Every task includes an exact repository path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm the product-image plan and current code/schema state before implementation.

- [x] T001 Verify `specs/001-merchant-onboarding/plan.md`, `specs/001-merchant-onboarding/research.md`, `specs/001-merchant-onboarding/data-model.md`, `specs/001-merchant-onboarding/contracts/product-catalog.md`, and `specs/001-merchant-onboarding/quickstart.md` all describe object-key persistence and signed retrieval URLs
- [x] T002 Inspect existing product image schema in `src/main/resources/db/changelog/20260420-002-merchant-product-images.sql`, `src/main/resources/db/changelog/20260420-003-product-images-storage-key.sql`, `src/main/resources/db/changelog/20260421-002-restore-merchant-product-image-url.sql`, and `src/main/resources/db/changelog/db.changelog-master.yaml` before deciding whether to rename `image_url` or map the existing `storage_key`
- [x] T003 Inspect current product and storage implementation in `src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantProduct.kt`, `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`, `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt`, `src/main/kotlin/com/elegant/software/blitzpay/storage/StorageService.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/storage/S3StorageService.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add the shared storage, metadata, validation, and error-handling primitives needed by all product-image stories.

**CRITICAL**: No user story implementation should start until this phase is complete.

- [x] T004 Add direct binary upload support to the storage boundary in `src/main/kotlin/com/elegant/software/blitzpay/storage/StorageService.kt`
- [x] T005 Implement direct S3/MinIO upload with `S3Client.putObject` in `src/main/kotlin/com/elegant/software/blitzpay/storage/S3StorageService.kt`
- [x] T006 [P] Add product image constants, accepted MIME types, max size, and extension mapping in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/ProductImagePolicy.kt`
- [x] T007 [P] Add a product image upload value object for validated multipart data in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/ProductImageUpload.kt`
- [x] T008 Rename or remap product image metadata from URL semantics to object-key semantics and add optional `description` in `src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantProduct.kt`
- [x] T009 Update product API models so response `imageUrl` remains generated-only, request bodies no longer accept client-provided `imageUrl`, and product models include optional `description` in `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantProductModels.kt`
- [x] T010 Create or adjust the Liquibase migration for image-key normalization and product description in `src/main/resources/db/changelog/20260421-003-product-image-storage-key-normalization.sql`
- [x] T011 Include the new image-key migration in `src/main/resources/db/changelog/db.changelog-master.yaml`
- [x] T012 Add structured storage upload/signing exception types or mapping helpers in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`

**Checkpoint**: Foundation ready. Domain, schema, storage boundary, and API model naming no longer treat the persisted product image value as a direct URL.

---

## Phase 3: User Story 1 - Multipart Product Create/Update With Image (Priority: P1) MVP

**Goal**: A merchant can create or update a product using multipart form fields, optional Markdown description, and an optional JPEG/PNG/WebP image; the server uploads the image to S3/MinIO and stores only the generated object key.

**Independent Test**: `POST /v1/merchants/{merchantId}/products` with `name`, optional `description`, `unitPrice`, and a valid image returns `201 Created`, persists the product description, calls storage upload with a key under `merchants/{merchantId}/products/{productId}/`, and stores only that key. `PUT /v1/merchants/{merchantId}/products/{productId}` with a valid image and description replaces the stored values and returns `200 OK`.

### Tests for User Story 1

> Write these tests first and verify they fail before implementation tasks T016-T019.

- [x] T013 [P] [US1] Add service test for multipart create with valid image storing an object key in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T014 [P] [US1] Add service test for multipart update with valid image replacing the stored key in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T015 [P] [US1] Add WebFlux contract test for multipart `POST /v1/merchants/{merchantId}/products` happy path in `src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantContractTest.kt`
- [x] T016 [P] [US1] Add service test for create/update preserving optional Markdown description in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`

### Implementation for User Story 1

- [x] T017 [US1] Change `MerchantProductController.create` to consume `multipart/form-data` parts `name`, optional `description`, `unitPrice`, and optional `image` in `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt`
- [x] T018 [US1] Change `MerchantProductController.update` to consume `multipart/form-data` parts `name`, optional `description`, `unitPrice`, and optional `image` in `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt`
- [x] T019 [US1] Implement create/update description handling, image orchestration, object key generation, and upload-before-save behavior in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [x] T020 [US1] Update product response mapping for create/update to include `description` and generated signed `imageUrl` values when an image key exists in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`

**Checkpoint**: User Story 1 is independently testable with multipart create/update and object-key persistence.

---

## Phase 4: User Story 2 - Retrieve Products With Signed Image URLs (Priority: P2)

**Goal**: Product list and get responses expose short-lived signed retrieval URLs generated by the API after product access checks pass.

**Independent Test**: `GET /v1/merchants/{merchantId}/products` and `GET /v1/merchants/{merchantId}/products/{productId}` return a signed `imageUrl` for products with an image key, return `imageUrl: null` when no image key exists or signing cannot resolve the object, and never return a stored direct storage URL.

### Tests for User Story 2

- [x] T021 [P] [US2] Add service test for list/get generating signed image URLs from stored keys in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T022 [P] [US2] Add service test for list/get returning `imageUrl = null` when no key exists or signing fails in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T023 [P] [US2] Add WebFlux contract test for `GET /v1/merchants/{merchantId}/products` signed image URL and description response in `src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantContractTest.kt`

### Implementation for User Story 2

- [x] T024 [US2] Update `MerchantProductService.list` to map stored image keys to signed retrieval URLs and include descriptions after tenant filtering in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [x] T025 [US2] Update `MerchantProductService.get` to map stored image keys to signed retrieval URLs and include descriptions after tenant filtering in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [x] T026 [US2] Add safe signed URL generation that logs key-level failures without exposing secrets and returns null for unresolved images in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`

**Checkpoint**: User Story 2 is independently testable with existing products that already have image keys.

---

## Phase 5: User Story 3 - Validate Image Uploads and Prevent Partial Writes (Priority: P3)

**Goal**: Unsupported image types, oversized images, and object storage failures are rejected with structured errors and do not leave partial product records or stale image updates.

**Independent Test**: Multipart create/update with `text/plain` image returns `400 Bad Request`; image larger than 5 MB returns `400 Bad Request`; simulated storage upload failure returns a storage error response and leaves product/database state unchanged.

### Tests for User Story 3

- [x] T027 [P] [US3] Add service tests for unsupported MIME type, oversized image, and overlong description rejection in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T028 [P] [US3] Add service tests proving storage upload failure does not persist create/update changes in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductServiceTest.kt`
- [x] T029 [P] [US3] Add WebFlux contract tests for multipart validation errors on create/update in `src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/MerchantContractTest.kt`

### Implementation for User Story 3

- [x] T030 [US3] Enforce MIME type, 5 MB size, and 2,000-character description validation before storage or repository calls in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [x] T031 [US3] Add best-effort object cleanup when DB persistence fails after a successful upload in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [x] T032 [US3] Map validation and storage failures to structured HTTP responses in `src/main/kotlin/com/elegant/software/blitzpay/merchant/web/MerchantProductController.kt`

**Checkpoint**: User Story 3 is independently testable with invalid image inputs and a failing mocked storage service.

---

## Phase 6: User Story 4 - Asynchronous Google Place ID Enrichment (Priority: P4)

**Goal**: Stored merchant and branch `googlePlaceId` values are validated and enriched asynchronously through Google Maps so location requests stay fast while address/reviews data becomes available later.

**Independent Test**: Saving a merchant or branch location with `googlePlaceId` does not call Google Maps synchronously, a scheduled job fetches unresolved Place IDs, persists enrichment metadata, and records retryable failure state when Google Maps is unavailable.

### Tests for User Story 4

- [x] T033 [P] [US4] Add service test for storing `googlePlaceId` without synchronous Google Maps validation in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantLocationServiceTest.kt`
- [x] T034 [P] [US4] Add service test for successful Place ID enrichment persisting address/reviews metadata in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantPlaceEnrichmentServiceTest.kt`
- [x] T035 [P] [US4] Add service test for Google Maps failure recording retryable enrichment failure state in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantPlaceEnrichmentServiceTest.kt`
- [x] T036 [P] [US4] Add branch service test for creating a branch with operational address and geolocation in `src/test/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantBranchServiceTest.kt`

### Implementation for User Story 4

- [x] T037 [US4] Add Google Maps enrichment configuration properties in `src/main/kotlin/com/elegant/software/blitzpay/merchant/config/GoogleMapsProperties.kt`
- [x] T038 [US4] Add Google Maps WebClient configuration and scheduling enablement in `src/main/kotlin/com/elegant/software/blitzpay/merchant/config/GoogleMapsConfig.kt`
- [x] T039 [US4] Extend merchant and branch location persistence with operational address and enrichment fields/status in `src/main/kotlin/com/elegant/software/blitzpay/merchant/domain/MerchantLocation.kt`
- [x] T040 [US4] Expose branch address and location fields in create/response models in `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantBranchModels.kt`
- [x] T041 [US4] Persist branch address and geolocation from branch create requests in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantBranchService.kt`
- [x] T042 [US4] Add Liquibase migration for merchant and branch location enrichment fields in `src/main/resources/db/changelog/20260421-004-merchant-location-place-enrichment.sql`
- [x] T043 [US4] Include the location enrichment migration in `src/main/resources/db/changelog/db.changelog-master.yaml`
- [x] T044 [US4] Add repository queries for merchant and branch records with unresolved Place IDs in `src/main/kotlin/com/elegant/software/blitzpay/merchant/repository/MerchantApplicationRepository.kt` and `src/main/kotlin/com/elegant/software/blitzpay/merchant/repository/MerchantBranchRepository.kt`
- [x] T045 [US4] Implement Google Maps Place Details client in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/GoogleMapsPlaceClient.kt`
- [x] T046 [US4] Implement scheduled Place ID enrichment job and retryable failure handling for merchant and branch locations in `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantPlaceEnrichmentService.kt`
- [x] T047 [US4] Add `blitzpay.google-maps` local configuration placeholders in `src/main/resources/application.yml`

**Checkpoint**: User Story 4 is independently testable with a mocked Google Maps client and does not require live Google Maps access in tests.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, OpenAPI, fixture alignment, and final verification.

- [ ] T048 [P] Update generated OpenAPI source contract for multipart product create/update, product description, signed `imageUrl` responses, branch location fields, and location enrichment fields in `api-docs/api-doc.yml`
- [ ] T049 [P] Update product catalog documentation examples to include optional Markdown description and remove client-provided `imageUrl` request fields in `specs/001-merchant-onboarding/contracts/product-catalog.md`
- [ ] T050 [P] Update location and branch contract documentation for branch address/geolocation and async Google Place ID enrichment in `specs/001-merchant-onboarding/contracts/merchant-location.md`
- [ ] T051 [P] Update local verification notes for product description, MinIO object upload, signed retrieval URLs, branch geolocation, and mocked Google Maps enrichment in `specs/001-merchant-onboarding/quickstart.md`
- [ ] T052 Run `./gradlew test` and fix failures before continuing
- [ ] T053 Run `./gradlew contractTest` and fix failures before continuing
- [ ] T054 Run `./gradlew clean build` and fix failures before considering the feature complete

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2; MVP
- **Phase 4 (US2)**: Depends on Phase 2 and can run after key response mapping exists, but is easiest after US1
- **Phase 5 (US3)**: Depends on Phase 2 and can run in parallel with US1/US2 after validation primitives exist
- **Phase 6 (US4)**: Depends on existing location save/read behavior; can run after Phase 2 because it is independent of product image stories
- **Phase 7 (Polish)**: Depends on selected user stories being complete

### User Story Dependencies

- **US1 (P1)**: No dependency on US2/US3; delivers multipart upload and object-key persistence
- **US2 (P2)**: Requires persisted image key semantics from Phase 2; can be tested with seeded products
- **US3 (P3)**: Requires validation and storage primitives from Phase 2; hardens create/update behavior
- **US4 (P4)**: Requires existing merchant location behavior; independent of product image stories

### Within Each User Story

- Tests first, then implementation
- Domain/storage/model changes before service orchestration
- Service behavior before controller contract completion
- Contract tests green before final build

---

## Parallel Opportunities

- T006 and T007 can run in parallel with storage implementation T004/T005 after Phase 1
- T013, T014, and T015 can be written in parallel for US1
- T020, T021, and T022 can be written in parallel for US2
- T026, T027, and T028 can be written in parallel for US3
- T032, T033, T034, and T035 can be written in parallel for US4
- T047, T048, T049, and T050 can run in parallel during polish

---

## Parallel Example: User Story 1

```text
Task T013: Add service test for multipart create with valid image in MerchantProductServiceTest.kt
Task T014: Add service test for multipart update with valid image in MerchantProductServiceTest.kt
Task T015: Add WebFlux contract test for multipart POST in MerchantContractTest.kt
```

## Parallel Example: User Story 2

```text
Task T020: Add signed URL generation service test in MerchantProductServiceTest.kt
Task T021: Add null imageUrl fallback service test in MerchantProductServiceTest.kt
Task T022: Add GET products signed imageUrl contract test in MerchantContractTest.kt
```

## Parallel Example: User Story 3

```text
Task T026: Add invalid MIME and oversized image service tests in MerchantProductServiceTest.kt
Task T027: Add storage failure rollback service tests in MerchantProductServiceTest.kt
Task T028: Add multipart validation error contract tests in MerchantContractTest.kt
```

## Parallel Example: User Story 4

```text
Task T032: Add no-synchronous-validation location service test in MerchantLocationServiceTest.kt
Task T033: Add successful Place ID enrichment service test in MerchantPlaceEnrichmentServiceTest.kt
Task T034: Add Google Maps failure retry-state service test in MerchantPlaceEnrichmentServiceTest.kt
Task T035: Add branch address/geolocation service test in MerchantBranchServiceTest.kt
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 setup verification.
2. Complete Phase 2 foundation: storage upload, image policy/value object, object-key metadata, schema migration.
3. Write US1 tests T013-T015 and confirm they fail.
4. Implement US1 tasks T016-T019.
5. Validate US1 with `./gradlew test --tests com.elegant.software.blitzpay.merchant.application.MerchantProductServiceTest` and the relevant contract test.

### Incremental Delivery

1. Deliver US1: multipart upload and object-key persistence.
2. Deliver US2: signed retrieval URLs for list/get/read responses.
3. Deliver US3: validation and partial-write protection.
4. Deliver US4: asynchronous Google Place ID enrichment.
5. Run full verification: `./gradlew test`, `./gradlew contractTest`, `./gradlew clean build`.

### Parallel Team Strategy

1. One developer completes storage and schema foundation.
2. After Phase 2, developers can split US1, US2, and US3 tests across separate files/sections.
3. Coordinate changes to `MerchantProductService.kt` and `MerchantProductController.kt` because those files are touched by multiple stories.

---

## Notes

- Keep object storage private; do not add public-read bucket requirements.
- Do not persist signed URLs; only persist stable object keys.
- Do not accept client-provided object keys for product images.
- Do not call Google Maps synchronously from `PUT /v1/merchants/{merchantId}/location`; enrichment runs in the scheduled background service.
- Keep blocking JPA and AWS SDK work off the WebFlux event loop by preserving `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
- Existing unrelated worktree changes must not be reverted while implementing these tasks.
