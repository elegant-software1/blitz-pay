# Implementation Plan: Merchant Product Image Upload API

**Branch**: `001-merchant-onboarding` | **Date**: 2026-04-21 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-merchant-onboarding/spec.md`

## Summary

Extend the existing merchant product catalog so product create/update accepts multipart product fields with optional rich-text/Markdown description and optional image file, uploads accepted images to S3-compatible object storage (MinIO locally), stores a stable image object key rather than a direct URL, and returns short-lived signed retrieval URLs in product API responses. The existing Spring Boot WebFlux + JPA service remains the architecture; blocking JPA/S3 SDK work stays wrapped on `Schedulers.boundedElastic()`.

## Technical Context

**Language/Version**: Kotlin 2.3.20 on Java 25  
**Primary Dependencies**: Spring Boot 4.0.4, Spring WebFlux, Spring Modulith, Spring Data JPA/Hibernate, Liquibase, AWS SDK S3 v2, Jackson Kotlin module, Bean Validation  
**Storage**: PostgreSQL 16 for product metadata; S3-compatible object storage for private product images (`blitzpay.storage.*`, MinIO in local env)  
**Testing**: `./gradlew test`, focused JUnit 5 service tests, WebFlux controller tests with `WebTestClient`, existing `contractTest` source set where API contract coverage is added  
**Target Platform**: JVM web service running on Spring Boot; local development against PostgreSQL and MinIO-compatible S3 endpoint  
**Project Type**: Web service / REST API  
**Performance Goals**: Product list/get should continue returning under existing onboarding API expectations; signed URL generation is local SDK work and should not introduce provider round trips. Multipart create/update should reject invalid images before DB write.  
**Constraints**: Product description is optional rich-text/Markdown up to 2,000 characters. Images are private; no public-read bucket policy. Accepted file types are JPEG, PNG, WebP only. Maximum image size is 5 MB. Product metadata stores a stable object key only. API returns `null` image field when an object key cannot be resolved.  
**Scale/Scope**: Single optional image per product for this increment; existing catalog endpoints remain under `/v1/merchants/{merchantId}/products`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The repository constitution file is still the placeholder template and contains no enforceable project-specific gates. Apply repository-level rules from `AGENTS.md` and the fixture policy already captured in `specs/004-fixture-test-policy`: focused changes, existing style, tests for behavior changes, no generated artifacts or secrets.

**Initial Gate Status**: PASS

| Gate | Result | Notes |
|------|--------|-------|
| Focused scope | PASS | Limits changes to merchant product image API, storage integration, contracts, and tests. |
| Testing | PASS | Requires service and WebFlux/contract coverage for multipart upload, validation, signed URL response, and storage failure behavior. |
| Security/privacy | PASS | Private objects and signed URLs; no public bucket access or secret leakage. |
| Architecture consistency | PASS | Reuses existing `storage` module and product catalog service/controller patterns. |

## Project Structure

### Documentation (this feature)

```text
specs/001-merchant-onboarding/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── product-catalog.md
│   └── merchant-location.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/elegant/software/blitzpay/
├── merchant/
│   ├── api/
│   │   └── MerchantProductModels.kt          # response/request model changes
│   ├── application/
│   │   └── MerchantProductService.kt         # multipart/image orchestration
│   ├── domain/
│   │   └── MerchantProduct.kt                # replace imageUrl with object key semantics
│   ├── repository/
│   │   └── MerchantProductRepository.kt
│   └── web/
│       └── MerchantProductController.kt      # multipart create/update
├── storage/
│   ├── StorageService.kt                     # add direct upload/head support if needed
│   └── S3StorageService.kt
└── resources/
    ├── application.yml
    └── db/changelog/
```

```text
src/test/kotlin/com/elegant/software/blitzpay/
├── merchant/application/
│   └── MerchantProductServiceTest.kt
└── merchant/web/
    └── MerchantProductControllerTest.kt

src/contractTest/kotlin/com/elegant/software/blitzpay/contract/
└── ContractVerifierBase.kt
```

**Structure Decision**: Single Spring Boot service. Keep the existing `merchant` module as owner of product behavior and use the existing `storage` module as the S3/MinIO boundary.

## Phase 0: Research

See [research.md](./research.md). All technical unknowns are resolved:

- Multipart product write shape in WebFlux
- Private S3/MinIO storage with short-lived signed GET URLs
- Object key persistence and response URL generation
- Upload validation and storage failure transaction behavior
- Local MinIO configuration reuse

## Phase 1: Design & Contracts

See generated design artifacts:

- [data-model.md](./data-model.md)
- [contracts/product-catalog.md](./contracts/product-catalog.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Check

**Gate Status**: PASS

| Gate | Result | Notes |
|------|--------|-------|
| Focused scope | PASS | Design touches only product image upload/read behavior and docs. |
| Testing | PASS | Quickstart and contracts identify validation, failure, and response cases for tests. |
| Security/privacy | PASS | Private objects with signed retrieval URLs; object keys only in persistence. |
| Architecture consistency | PASS | Reuses current Spring WebFlux controller, boundedElastic blocking boundary, JPA repository, Liquibase, and AWS S3 SDK setup. |

## Complexity Tracking

No constitution violations or added architectural complexity requiring justification.
