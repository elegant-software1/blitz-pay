# Research: Merchant Onboarding — Skip Onboarding, Register Merchant

**Branch**: `001-merchant-onboarding` | **Phase**: 0 | **Date**: 2026-03-29

## Decision Log

### Decision 1: Skip Full Onboarding Workflow

- **Decision**: Bypass the multi-step onboarding workflow (DRAFT → SUBMITTED → VERIFICATION → SCREENING → RISK_REVIEW → DECISION_PENDING → SETUP → ACTIVE) and provide a direct merchant registration path.
- **Rationale**: The full KYB/AML compliance review pipeline requires external integrations (screening providers, manual reviewer tooling) that are not yet in place. A direct registration endpoint unblocks downstream feature work (payments, invoicing) by getting a merchant into `ACTIVE` state immediately.
- **Alternatives considered**: Implementing the full onboarding flow first — rejected because it has no unblocking value until screening and reviewer tools exist. Adding a `REGISTERED` status — rejected as it adds a status that doesn't appear in the spec lifecycle; direct activation is cleaner.

---

### Decision 2: Lifecycle Extension for Direct Registration

- **Decision**: Add a `DRAFT → ACTIVE` transition to `MerchantOnboardingLifecycle` specifically for the "register direct" code path, gated by a method on `MerchantApplication` (`registerDirect()`). The lifecycle object remains the single source of truth.
- **Rationale**: Avoids bypassing the lifecycle enforcement entirely. The new transition is only reachable via `registerDirect()` — not through the generic `transitionTo()` path — so future lifecycle validators can restrict it easily.
- **Alternatives considered**: Setting `status = ACTIVE` directly in the service without a lifecycle transition — rejected because it would silently bypass `MerchantOnboardingLifecycle.requireTransition()`, which is the module's invariant guard.

---

### Decision 3: New `MerchantRegistrationService` (Separate from `MerchantOnboardingService`)

- **Decision**: Introduce a thin `MerchantRegistrationService` that owns the "create + activate" path. `MerchantOnboardingService` continues to own the lifecycle-transition operations.
- **Rationale**: Separation of concerns: registration creates a new merchant record; onboarding advances an existing application through review stages. Mixing them would conflate creation with lifecycle management.
- **Alternatives considered**: Adding a `register()` method to `MerchantOnboardingService` — rejected because that service's contract is lifecycle transitions, not creation.

---

### Decision 4: REST Controller `MerchantController`

- **Decision**: Create `MerchantController` at `merchant/api/` exposing:
  - `POST /v1/merchants` — register a merchant (creates + activates)
  - `GET /v1/merchants/{merchantId}` — retrieve merchant details
- **Rationale**: These are the minimal endpoints needed to prove the merchant module is functional end-to-end. No onboarding-specific endpoints are added yet.
- **Alternatives considered**: Exposing under `/v1/merchant-onboarding/` — rejected; the "skip onboarding" decision means the path should reflect simple registration, not a multi-step onboarding flow.

---

### Decision 5: `applicationReference` Generation

- **Decision**: Generate `applicationReference` as `"BLTZ-" + UUID.randomUUID().toString().take(8).uppercase()` in `MerchantRegistrationService`.
- **Rationale**: Produces a short, human-readable reference (e.g., `BLTZ-A3F2C9E1`) consistent with the field already modeled on `MerchantApplication`.
- **Alternatives considered**: Sequence-based integer references — requires a DB sequence; deferred for later polish.

---

### Decision 6: Request / Response Models

- **Decision**: Reuse `MerchantBusinessProfileRequest` and `MerchantPrimaryContactRequest` from `MerchantOnboardingModels.kt` for the register request. Add a minimal `RegisterMerchantRequest` wrapper. Response reuses `MerchantApplicationResponse`.
- **Rationale**: Models are already defined and correct. Adding a thin wrapper avoids creating a parallel type hierarchy.
- **Alternatives considered**: New dedicated request/response types — unnecessary duplication given the existing models cover the required fields.

---

### Decision 7: Contract Test Profile

- **Decision**: The contract test profile (`contract-test`) already mocks DataSource and JPA. The new controller test will follow the same pattern as existing contract tests (e.g., `InvoiceControllerTest`).
- **Rationale**: Consistent testing strategy. No real DB required for controller-layer contract tests.
- **Alternatives considered**: Integration test with real DB — appropriate for `MerchantRegistrationService` but not for the controller contract test.
