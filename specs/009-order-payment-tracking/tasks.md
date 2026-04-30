
# Tasks: Order Payment Tracking

**Input**: Design documents from `/specs/009-order-payment-tracking/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are required for this feature because the specification defines independent test scenarios and the repository constitution requires coverage for every behavior change.

**Organization**: Tasks are grouped by user story so each increment can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unmet dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Every task below includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the module and test scaffolding needed for the order feature.

- [ ] T001 Create the order module package declarations in `src/main/kotlin/com/elegant/software/blitzpay/order/package-info.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/api/package-info.kt`
- [ ] T002 [P] Create order test fixture directories and baseline JSON files in `src/test/resources/testdata/order/create-order/` and `src/test/resources/testdata/order/payment-status/`
- [ ] T003 [P] Create the order test package scaffolding in `src/test/kotlin/com/elegant/software/blitzpay/order/` and `src/contractTest/kotlin/com/elegant/software/blitzpay/order/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the schema, module contracts, and shared services that every user story depends on.

**⚠️ CRITICAL**: No user story work should start before this phase is complete.

- [ ] T004 Add the order schema changes and master registration in `src/main/resources/db/changelog/20260430-001-create-order-tables.sql` and `src/main/resources/db/changelog/db.changelog-master.yaml`
- [ ] T005 [P] Create the order persistence entities in `src/main/kotlin/com/elegant/software/blitzpay/order/domain/Order.kt`, `src/main/kotlin/com/elegant/software/blitzpay/order/domain/OrderItem.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/order/domain/PaymentAttempt.kt`
- [ ] T006 [P] Create the order repositories in `src/main/kotlin/com/elegant/software/blitzpay/order/repository/OrderRepository.kt`, `src/main/kotlin/com/elegant/software/blitzpay/order/repository/OrderItemRepository.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/order/repository/PaymentAttemptRepository.kt`
- [ ] T007 [P] Extend merchant-facing lookup contracts for orderable product snapshots in `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantGateway.kt`, `src/main/kotlin/com/elegant/software/blitzpay/merchant/api/MerchantProductModels.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/merchant/application/MerchantProductService.kt`
- [ ] T008 Create the order module API and service skeletons in `src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderGateway.kt`, `src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderModels.kt`, `src/main/kotlin/com/elegant/software/blitzpay/order/application/OrderService.kt`, `src/main/kotlin/com/elegant/software/blitzpay/order/application/PaymentAttemptService.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/order/application/OrderStatusProjection.kt`
- [ ] T009 Update modulith verification coverage for the new order module in `src/test/kotlin/com/elegant/software/blitzpay/ModularityTest.kt` and `src/test/kotlin/com/elegant/software/blitzpay/order/OrderModularityTest.kt`

**Checkpoint**: Order schema, module boundaries, and shared APIs are ready; user stories can now proceed.

---

## Phase 3: User Story 1 - Receive Orders From Merchant Products (Priority: P1) 🎯 MVP

**Goal**: Accept orders built from merchant-owned products and persist a single order with item snapshots and totals.

**Independent Test**: Submit a valid `POST /v1/orders` request and verify one order is created with a unique order ID, merchant ownership, item snapshots, and calculated totals; submit invalid products and verify the request fails with a clear validation response.

### Tests for User Story 1

- [ ] T010 [P] [US1] Add contract coverage for `POST /v1/orders` in `src/contractTest/kotlin/com/elegant/software/blitzpay/order/OrderContractTest.kt`
- [ ] T011 [P] [US1] Add order-creation service tests with fixture data in `src/test/kotlin/com/elegant/software/blitzpay/order/application/OrderServiceTest.kt` and `src/test/resources/testdata/order/create-order/`

### Implementation for User Story 1

- [ ] T012 [P] [US1] Implement create-order request and response models in `src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderModels.kt`
- [ ] T013 [P] [US1] Implement order item snapshot mapping and total calculation in `src/main/kotlin/com/elegant/software/blitzpay/order/domain/Order.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/domain/OrderItem.kt`
- [ ] T014 [US1] Implement merchant product validation and order-creation orchestration in `src/main/kotlin/com/elegant/software/blitzpay/order/application/OrderService.kt`
- [ ] T015 [US1] Persist created orders and captured line items through `src/main/kotlin/com/elegant/software/blitzpay/order/repository/OrderRepository.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/repository/OrderItemRepository.kt`
- [ ] T016 [US1] Expose `POST /v1/orders` and map validation failures in `src/main/kotlin/com/elegant/software/blitzpay/order/web/OrderController.kt`

**Checkpoint**: User Story 1 is independently functional as the MVP for order intake.

---

## Phase 4: User Story 2 - Track Payment Delivery Through Order Status (Priority: P1)

**Goal**: Make the order status the authoritative business view of payment delivery and keep it queryable over time.

**Independent Test**: Create an order, apply payment status changes, and verify `GET /v1/orders/{orderId}` shows the latest business status while rejecting stale or regressive updates after the order is paid.

### Tests for User Story 2

- [ ] T017 [P] [US2] Extend contract coverage for `GET /v1/orders/{orderId}` in `src/contractTest/kotlin/com/elegant/software/blitzpay/order/OrderContractTest.kt`
- [ ] T018 [P] [US2] Add status-transition and stale-update tests in `src/test/kotlin/com/elegant/software/blitzpay/order/application/OrderStatusProjectionTest.kt` and `src/test/resources/testdata/order/payment-status/`

### Implementation for User Story 2

- [ ] T019 [P] [US2] Implement the order payment lifecycle and transition guards in `src/main/kotlin/com/elegant/software/blitzpay/order/domain/OrderStatus.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/domain/Order.kt`
- [ ] T020 [US2] Implement read models and the `GET /v1/orders/{orderId}` endpoint in `src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderModels.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/web/OrderController.kt`
- [ ] T021 [US2] Project `payments.push.api.PaymentStatusChanged` events onto orders in `src/main/kotlin/com/elegant/software/blitzpay/order/application/OrderStatusProjection.kt`
- [ ] T022 [US2] Persist payment attempts and enforce terminal-state protection in `src/main/kotlin/com/elegant/software/blitzpay/order/domain/PaymentAttempt.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/repository/PaymentAttemptRepository.kt`

**Checkpoint**: User Story 2 is independently testable with durable order status tracking and safe transition rules.

---

## Phase 5: User Story 3 - Reuse Order ID As The Payment Reference (Priority: P2)

**Goal**: Reuse the generated order ID as the canonical reference across payment providers and payment attempts.

**Independent Test**: Create an order, initiate payments through the supported QR/TrueLayer, Braintree, and Stripe flows, and verify each provider path links back to the same order ID and stores a payment-attempt association for later callbacks.

### Tests for User Story 3

- [ ] T023 [P] [US3] Extend payment contract coverage for required order references in `src/contractTest/kotlin/com/elegant/software/blitzpay/contract/PaymentsContractTest.kt`, `src/contractTest/kotlin/com/elegant/software/blitzpay/payments/braintree/BraintreePaymentControllerContractTest.kt`, and `src/contractTest/kotlin/com/elegant/software/blitzpay/payments/stripe/StripePaymentControllerContractTest.kt`
- [ ] T024 [P] [US3] Add payment-attempt linking tests in `src/test/kotlin/com/elegant/software/blitzpay/order/application/PaymentAttemptServiceTest.kt`, `src/test/kotlin/com/elegant/software/blitzpay/payments/braintree/BraintreePaymentServiceTest.kt`, and `src/test/kotlin/com/elegant/software/blitzpay/payments/stripe/StripePaymentServiceTest.kt`

### Implementation for User Story 3

- [ ] T025 [US3] Implement payable-order checks and payment-attempt registration in `src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderGateway.kt` and `src/main/kotlin/com/elegant/software/blitzpay/order/application/PaymentAttemptService.kt`
- [ ] T026 [US3] Require a pre-created `orderId` in QR payment initiation and initialization flow in `src/main/kotlin/com/elegant/software/blitzpay/payments/qrpay/PaymentRequestController.kt` and `src/main/kotlin/com/elegant/software/blitzpay/payments/qrpay/PaymentInitRequestListener.kt`
- [ ] T027 [US3] Propagate the canonical `orderId` through Braintree checkout in `src/main/kotlin/com/elegant/software/blitzpay/payments/braintree/internal/BraintreePaymentController.kt` and `src/main/kotlin/com/elegant/software/blitzpay/payments/braintree/internal/BraintreePaymentService.kt`
- [ ] T028 [US3] Propagate the canonical `orderId` through Stripe intent creation and linked status initialization in `src/main/kotlin/com/elegant/software/blitzpay/payments/stripe/internal/StripePaymentController.kt`, `src/main/kotlin/com/elegant/software/blitzpay/payments/stripe/internal/StripePaymentService.kt`, and `src/main/kotlin/com/elegant/software/blitzpay/payments/push/internal/PaymentStatusService.kt`
- [ ] T029 [US3] Align TrueLayer request metadata with order-linked payment attempts in `src/main/kotlin/com/elegant/software/blitzpay/payments/truelayer/api/PaymentGateway.kt` and `src/main/kotlin/com/elegant/software/blitzpay/payments/truelayer/outbound/PaymentService.kt`

**Checkpoint**: User Story 3 is independently testable with one canonical order reference across supported payment providers.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish documentation and final validation across all stories.

- [ ] T030 [P] Update the feature contract notes and rollout guidance in `specs/009-order-payment-tracking/contracts/payment-order-reference.json` and `specs/009-order-payment-tracking/quickstart.md`
- [ ] T031 Validate end-to-end module and contract coverage in `src/test/kotlin/com/elegant/software/blitzpay/ModularityTest.kt` and `src/contractTest/kotlin/com/elegant/software/blitzpay/order/OrderContractTest.kt`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** starts immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user stories.
- **Phase 3: US1** depends on Phase 2 and delivers the MVP.
- **Phase 4: US2** depends on Phase 2 and builds on the order aggregate introduced for US1.
- **Phase 5: US3** depends on Phases 2, 3, and 4 because payment-reference reuse relies on persisted orders and durable status tracking.
- **Phase 6: Polish** depends on the stories selected for delivery.

### User Story Dependencies

- **US1**: No dependency on later stories; this is the first shippable increment.
- **US2**: Depends on the shared order aggregate and repository work from Phase 2; it extends US1’s persisted order lifecycle.
- **US3**: Depends on order creation and status-tracking infrastructure so provider flows can validate and link payment attempts safely.

### Within Each User Story

- Contract and service tests should be written first and should fail before implementation.
- Domain and DTO work comes before service orchestration.
- Service orchestration comes before controller or provider integration wiring.
- A story is complete only when its independent test passes.

### Parallel Opportunities

- **Setup**: T002 and T003 can run in parallel after T001.
- **Foundational**: T005, T006, and T007 can run in parallel after T004.
- **US1**: T010 and T011 can run in parallel; T012 and T013 can run in parallel before T014.
- **US2**: T017 and T018 can run in parallel; T019 can proceed in parallel with early read-model drafting before T020.
- **US3**: T023 and T024 can run in parallel; provider-specific tasks T027, T028, and T029 can be split once T025 and T026 are in place.

---

## Parallel Example: User Story 1

```bash
# Write the US1 tests in parallel:
Task: "Add contract coverage for POST /v1/orders in src/contractTest/kotlin/com/elegant/software/blitzpay/order/OrderContractTest.kt"
Task: "Add order-creation service tests in src/test/kotlin/com/elegant/software/blitzpay/order/application/OrderServiceTest.kt"

# Build the US1 data model in parallel:
Task: "Implement create-order request and response models in src/main/kotlin/com/elegant/software/blitzpay/order/api/OrderModels.kt"
Task: "Implement order item snapshot mapping and total calculation in src/main/kotlin/com/elegant/software/blitzpay/order/domain/Order.kt and src/main/kotlin/com/elegant/software/blitzpay/order/domain/OrderItem.kt"
```

---

## Parallel Example: User Story 3

```bash
# Update payment contracts in parallel:
Task: "Extend payment contract coverage in src/contractTest/kotlin/com/elegant/software/blitzpay/contract/PaymentsContractTest.kt"
Task: "Extend Braintree order-reference coverage in src/contractTest/kotlin/com/elegant/software/blitzpay/payments/braintree/BraintreePaymentControllerContractTest.kt"
Task: "Extend Stripe order-reference coverage in src/contractTest/kotlin/com/elegant/software/blitzpay/payments/stripe/StripePaymentControllerContractTest.kt"

# Split provider wiring once the shared payment-attempt service exists:
Task: "Propagate the canonical orderId through Braintree checkout in src/main/kotlin/com/elegant/software/blitzpay/payments/braintree/internal/BraintreePaymentController.kt and src/main/kotlin/com/elegant/software/blitzpay/payments/braintree/internal/BraintreePaymentService.kt"
Task: "Propagate the canonical orderId through Stripe intent creation in src/main/kotlin/com/elegant/software/blitzpay/payments/stripe/internal/StripePaymentController.kt and src/main/kotlin/com/elegant/software/blitzpay/payments/stripe/internal/StripePaymentService.kt"
Task: "Align TrueLayer request metadata in src/main/kotlin/com/elegant/software/blitzpay/payments/truelayer/api/PaymentGateway.kt and src/main/kotlin/com/elegant/software/blitzpay/payments/truelayer/outbound/PaymentService.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1.
2. Complete Phase 2.
3. Complete Phase 3.
4. Validate `POST /v1/orders` independently before expanding the payment lifecycle.

### Incremental Delivery

1. Deliver US1 for order intake.
2. Add US2 for queryable payment-delivery status.
3. Add US3 to unify payment references across providers.
4. Finish with Phase 6 documentation and final verification.

### Parallel Team Strategy

1. One developer handles schema and repositories while another handles merchant API exposure during Phase 2.
2. After Phase 2, one developer can own the order HTTP surface while another owns status projection tests.
3. In US3, provider-specific Braintree, Stripe, and TrueLayer wiring can be split across developers once the shared payment-attempt service exists.

---

## Notes

- `[P]` tasks touch different files and do not rely on incomplete work in the same phase.
- `[US1]`, `[US2]`, and `[US3]` preserve traceability back to the feature specification.
- The suggested MVP scope is **User Story 1 only**.
- All tasks above follow the required checklist format: checkbox, task ID, optional `[P]`, required story label for story tasks, and explicit file path.
