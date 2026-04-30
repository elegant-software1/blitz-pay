# Implementation Plan: Order Payment Tracking

**Branch**: `[009-order-payment-tracking]` | **Date**: 2026-04-30 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-order-payment-tracking/spec.md`

## Summary

Introduce a dedicated `order` business module that accepts orders built from merchant products, persists an order lifecycle with payment-oriented status, and standardizes the system-generated order ID as the shared payment reference across payment providers. The design keeps merchant catalog ownership in the `merchant` module, keeps payment execution in the `payments` module, and lets the new `order` module own order persistence, status transitions, and payment-attempt linkage.

## Technical Context

**Language/Version**: Kotlin 2.3.20 on Java 25  
**Primary Dependencies**: Spring Boot 4.0.4, Spring WebFlux, Spring Modulith, Spring Data JPA/Hibernate, Liquibase, Jackson Kotlin module, Bean Validation  
**Storage**: PostgreSQL 16 in schema `blitzpay`  
**Testing**: JUnit 5, Mockito Kotlin, WebTestClient contract tests, Spring Modulith verification tests  
**Target Platform**: Linux server on JVM  
**Project Type**: REST web-service with Spring Modulith business modules  
**Performance Goals**: 95% of valid order submissions complete in under 3 seconds; status lookup stays under 500ms for normal order volumes  
**Constraints**: `ddl-auto` remains `none`/`validate`; Liquibase owns schema; all public HTTP changes require contract tests; cross-module collaboration stays on published `api` interfaces and domain events; input validation happens at HTTP boundaries  
**Scale/Scope**: Hundreds of merchants, thousands of products, low-to-moderate order volume in v1, multiple payment providers reusing the same order ID reference

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule | Status | Notes |
|------|--------|-------|
| Kotlin-only application code | ✅ | No Java source planned |
| Spring Modulith boundaries respected | ✅ | New top-level `order` module owns its own tables, exposes a dedicated `api` surface, and consumes published payment events |
| Cross-module access uses published contracts only | ✅ | Product validation uses `merchant.api`; payment outcome projection listens to `payments.push.api.PaymentStatusChanged` |
| API versioning and stable contracts | ✅ | New endpoints stay under `/v1/...`; payment endpoint contract tightening is documented and must be covered by contract tests |
| Liquibase owns schema in `blitzpay` | ✅ | New order tables and indexes are introduced via Liquibase only |
| Table prefix matches owning leaf module | ✅ | Tables and indexes will use the `order_` prefix |
| Tests for every behavior change | ✅ | Unit, contract, and modulith verification updates are part of the plan; `./gradlew check` remains the local gate |
| Thin controllers and validated boundaries | ✅ | Validation stays in web/request models; business rules stay in `order` services |

## Project Structure

### Documentation (this feature)

```text
specs/009-order-payment-tracking/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── create-order.json
│   ├── get-order.json
│   └── payment-order-reference.json
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/elegant/software/blitzpay/
├── order/
│   ├── api/
│   ├── application/
│   ├── domain/
│   ├── repository/
│   └── web/
├── merchant/
│   └── api/
├── payments/
│   ├── braintree/
│   ├── push/api/
│   ├── qrpay/
│   ├── stripe/
│   └── truelayer/api/
└── config/

src/main/resources/db/changelog/
└── changes/

src/test/kotlin/com/elegant/software/blitzpay/
├── order/
├── payments/
└── ModularityTest.kt

src/contractTest/kotlin/com/elegant/software/blitzpay/
├── contract/
├── merchant/
├── order/
└── payments/
```

**Structure Decision**: Use a new `order` leaf module because order lifecycle ownership is distinct from merchant catalog ownership and payment execution. The module will depend only on published module APIs for synchronous validation and on published payment events for asynchronous status progression.

## Phase 0: Research Summary

Phase 0 resolved the main design unknowns:
- module ownership belongs in a new `order` module rather than `merchant`, `payments`, or `invoice`
- merchant product validation should use a dedicated published merchant API surface
- order items must snapshot product data at creation time for historical accuracy
- order status stays business-facing with a compact payment lifecycle
- payment status projection should map normalized payment events onto order status using a persisted payment-attempt link
- the generated `orderId` is the canonical payment reference across providers

See [research.md](research.md) for the decision log and alternatives considered.

## Post-Design Constitution Check

| Rule | Status | Notes |
|------|--------|-------|
| Module boundaries remain explicit | ✅ | `order` owns persistence; `merchant` remains catalog owner; `payments` remains execution owner |
| No cross-module table access | ✅ | Order creation uses merchant snapshots via `merchant.api`; payment linkage is stored in `order` tables |
| Contract changes documented | ✅ | `POST /v1/orders`, `GET /v1/orders/{orderId}`, and payment reference expectations are captured under `contracts/` |
| Schema evolution follows Liquibase rules | ✅ | Data model defines new `order_`-prefixed tables plus indexes under the `blitzpay` schema |
| Testing strategy covers new behavior | ✅ | Quickstart includes unit, contract, and modulith verification expectations |

## Complexity Tracking

No constitution violations are currently required.
