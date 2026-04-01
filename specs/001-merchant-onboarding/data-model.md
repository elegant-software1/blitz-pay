# Data Model: Merchant Registration (Skip Onboarding)

**Branch**: `001-merchant-onboarding` | **Phase**: 1 | **Date**: 2026-03-29

## Scope

This document covers only the data model changes required for the "skip onboarding, register merchant" scope. The full onboarding entity model (SupportingMaterial, ReviewDecision, RiskAssessment, MonitoringRecord) is deferred.

---

## Existing Entities (No Schema Change)

| Entity | Table | Status |
|--------|-------|--------|
| `MerchantApplication` | `merchant_applications` | Exists — used as-is |
| `BusinessProfile` | Embedded in `merchant_applications` | Exists — used as-is |
| `PrimaryContact` | Embedded in `merchant_applications` | Exists — used as-is |
| `Person` | `merchant_people` | Exists — not used in registration path |
| `SupportingMaterial` | `merchant_supporting_materials` | Exists — not used in registration path |
| `ReviewDecision` | `merchant_review_decisions` | Exists — not used in registration path |
| `RiskAssessment` | Embedded in `merchant_applications` | Exists — null in registration path |
| `MonitoringRecord` | `merchant_monitoring_records` | Exists — null in registration path |

---

## Lifecycle Change

### `MerchantOnboardingLifecycle`

Add the `DRAFT → ACTIVE` transition to the transition map:

```
DRAFT → { SUBMITTED, ACTIVE }   ← add ACTIVE here
```

This is only reachable via `MerchantApplication.registerDirect()`. The generic `transitionTo()` uses the same map, so this transition becomes available through both paths — the service layer is responsible for only invoking `registerDirect()` for admin registration use cases.

---

## New Domain Method

### `MerchantApplication.registerDirect(activatedAt: Instant)`

```kotlin
fun registerDirect(activatedAt: Instant = Instant.now()) {
    MerchantOnboardingLifecycle.requireTransition(status, MerchantOnboardingStatus.ACTIVE)
    status = MerchantOnboardingStatus.ACTIVE
    submittedAt = activatedAt
    touch(activatedAt)
}
```

---

## New Service: `MerchantRegistrationService`

**Location**: `merchant/application/MerchantRegistrationService.kt`

| Method | Input | Output | Description |
|--------|-------|--------|-------------|
| `register(request)` | `RegisterMerchantCommand` | `MerchantApplication` | Creates application + activates directly |
| `findById(id)` | `UUID` | `MerchantApplication` | Retrieves by primary key |

### `RegisterMerchantCommand`

```kotlin
data class RegisterMerchantCommand(
    val businessProfile: BusinessProfile,
    val primaryContact: PrimaryContact,
    val registeredAt: Instant = Instant.now()
)
```

---

## Request / Response Models (API layer)

### `RegisterMerchantRequest` (new, in `MerchantOnboardingModels.kt`)

```kotlin
data class RegisterMerchantRequest(
    val businessProfile: MerchantBusinessProfileRequest,
    val primaryContact: MerchantPrimaryContactRequest
)
```

### `MerchantApplicationResponse` (existing — reused as-is)

Fields already cover: `applicationId`, `applicationReference`, `status`, `businessProfile`, `primaryContact`, `people`, `supportingMaterials`, `submittedAt`, `lastUpdatedAt`.

---

## Validation Rules

| Field | Rule |
|-------|------|
| `legalBusinessName` | Non-blank |
| `registrationNumber` | Non-blank; no duplicate active merchant for same registration number |
| `operatingCountry` | Non-blank, 2-letter ISO 3166-1 alpha-2 code recommended |
| `primaryBusinessAddress` | Non-blank |
| `primaryContact.fullName` | Non-blank |
| `primaryContact.email` | Non-blank, valid email format |
| `primaryContact.phoneNumber` | Non-blank |

---

## No New Database Tables

All tables already exist via Hibernate `ddl-auto: update`. No schema migration required.
