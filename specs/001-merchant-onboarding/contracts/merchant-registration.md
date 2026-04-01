# API Contract: Merchant Registration

**Module**: `merchant`
**Base path**: `/v1/merchants`
**Controller**: `MerchantController` (to be created at `merchant/api/MerchantController.kt`)

---

## POST /v1/merchants

Register a merchant directly, bypassing the multi-step onboarding workflow. The merchant is created and immediately set to `ACTIVE`.

### Request

```http
POST /v1/merchants
Content-Type: application/json
```

```json
{
  "businessProfile": {
    "legalBusinessName": "Acme GmbH",
    "businessType": "LLC",
    "registrationNumber": "DE123456789",
    "operatingCountry": "DE",
    "primaryBusinessAddress": "Hauptstraße 1, 10115 Berlin, Germany"
  },
  "primaryContact": {
    "fullName": "Jane Doe",
    "email": "jane.doe@acme.de",
    "phoneNumber": "+49301234567"
  }
}
```

### Success Response — 201 Created

```json
{
  "applicationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "applicationReference": "BLTZ-A3F2C9E1",
  "status": "ACTIVE",
  "businessProfile": {
    "legalBusinessName": "Acme GmbH",
    "businessType": "LLC",
    "registrationNumber": "DE123456789",
    "operatingCountry": "DE",
    "primaryBusinessAddress": "Hauptstraße 1, 10115 Berlin, Germany"
  },
  "primaryContact": {
    "fullName": "Jane Doe",
    "email": "jane.doe@acme.de",
    "phoneNumber": "+49301234567"
  },
  "people": [],
  "supportingMaterials": [],
  "submittedAt": "2026-03-29T10:00:00Z",
  "lastUpdatedAt": "2026-03-29T10:00:00Z"
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Required fields missing or invalid format |
| `409 Conflict` | Active merchant already exists for the same `registrationNumber` |

---

## GET /v1/merchants/{merchantId}

Retrieve a registered merchant by its UUID.

### Request

```http
GET /v1/merchants/{merchantId}
```

| Path parameter | Type | Description |
|----------------|------|-------------|
| `merchantId` | UUID | The `applicationId` returned at registration |

### Success Response — 200 OK

Same body shape as the 201 response above.

### Error Responses

| Status | Condition |
|--------|-----------|
| `404 Not Found` | No merchant found for the given `merchantId` |
