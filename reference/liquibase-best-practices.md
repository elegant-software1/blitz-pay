# Liquibase Best Practices

Authoritative reference for database schema migration conventions in this project.
See `CONTRIBUTING.md` for the link to this document.

---

## Current State

This project currently uses **Hibernate `ddl-auto: update`** for schema management.
This is acceptable in early development but must be migrated to Liquibase before going
to production with real data.

`ddl-auto: update` risks:
- Silent column/table drops on rename
- No rollback capability
- No audit trail of schema changes
- Cannot replay schema history on a fresh database reproducibly

---

## 1. Migration from `ddl-auto: update` to Liquibase

### Step 1 — Add Liquibase dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.liquibase:liquibase-core")
}
```

### Step 2 — Generate baseline from existing schema

Export the current schema from your running PostgreSQL database as the baseline
changelog. This represents "everything that exists before Liquibase took over."

```bash
# On the server — dump current schema (no data)
pg_dump --schema-only --no-owner --no-acl \
  -U postgres quickpay_db > baseline.sql
```

Convert to a Liquibase-formatted changelog and place it at:
```
src/main/resources/db/changelog/0001-baseline.sql
```

Mark the baseline changeset with `runOnChange: false` and `runAlways: false`.

### Step 3 — Update `application.yml`

All application objects live in a dedicated `blitzpay` schema, never `public`.
`public` is reserved for PostgreSQL extensions (`uuid-ossp`, `pg_trgm`, etc.).
The `DATABASECHANGELOG` / `DATABASECHANGELOGLOCK` tables sit alongside the app
tables in `blitzpay`.

```yaml
spring:
  datasource:
    hikari:
      connection-init-sql: "SET search_path TO blitzpay, public"
  jpa:
    hibernate:
      ddl-auto: validate   # ← change from 'update' to 'validate'
    properties:
      hibernate:
        default_schema: blitzpay
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
    default-schema: blitzpay
    liquibase-schema: blitzpay
```

`validate` lets Hibernate check that the schema matches the entity model,
but never modifies the schema. Liquibase owns all schema changes from now on.

### Step 4 — Bootstrap the `blitzpay` schema

The first changeset must create the schema before any table goes into it.
Keep it separate from the baseline dump so the baseline can be regenerated
without losing the schema-creation step.

```sql
-- liquibase formatted sql

-- changeset dev:0000-create-app-schema runAlways:false
CREATE SCHEMA IF NOT EXISTS blitzpay;
-- rollback DROP SCHEMA blitzpay;
```

When running `pg_dump` for the baseline (Step 2), dump only the `blitzpay`
schema so extension objects in `public` don't bleed into the baseline:

```bash
pg_dump --schema-only --no-owner --no-acl \
  --schema=blitzpay \
  -U postgres quickpay_db > baseline.sql
```

---

## 2. Changelog File Structure

Use a master changelog that includes individual versioned files.

```
src/main/resources/db/
└── changelog/
    ├── db.changelog-master.yaml     ← master index, includes all others
    ├── 0001-baseline.sql            ← initial schema snapshot
    ├── 0002-add-invoice-table.sql
    ├── 0003-add-payment-status-index.sql
    └── 0004-rename-order-id-column.sql
```

**`db.changelog-master.yaml`:**

```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/
      relativeToChangelogFile: false
      filter: liquibase.resource.DirectoryResourceAccessor
```

Or use explicit includes for deterministic ordering:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/0001-baseline.sql
  - include:
      file: db/changelog/0002-add-invoice-table.sql
```

---

## 3. Changeset Naming Convention

```
{sequence}-{description}.sql
```

- `{sequence}` — zero-padded 4-digit number: `0001`, `0002`, `0100`
- `{description}` — short kebab-case description of what the migration does

Examples:
```
0001-baseline.sql
0002-add-invoice-table.sql
0003-add-payment-request-status-index.sql
0004-rename-buyer-tax-id-to-vat-id.sql
0050-add-webhook-events-table.sql
```

---

## 4. Table and Index Naming Convention

Tables are owned by exactly one Spring Modulith **leaf** module and are prefixed with that module's identifier. This keeps ownership visible in the schema, aligns with the "each direct sub-package is a module" rule (`CONSTITUTION.md` → Module Boundaries), and prevents accidental cross-module coupling at the data layer.

**Format:** `{leaf_module}_{table}`

| Leaf module package | Prefix | Example tables |
|---|---|---|
| `payments.push` | `push_` | `push_device_registration`, `push_payment_status`, `push_delivery_attempt`, `push_processed_webhook_event` |
| `payments.qrpay` | `qrpay_` | `qrpay_request`, `qrpay_session` |
| `payments.truelayer` | `truelayer_` | `truelayer_payment`, `truelayer_webhook_event` |
| `invoice` | `invoice_` | `invoice_document`, `invoice_line_item` |

Rules:
- A table may only be read/written by its owning module. Cross-module consumers go through the owning module's `api` surface (named interfaces, events).
- Use the **leaf** module, not the parent — `push_` not `payments_`. This mirrors how modules are actually enforced by Modulith verification.
- Pick a consistent prefix per leaf and keep it short (one token). Do not reuse the same prefix across modules.

**Index naming** mirrors the table prefix:
- Non-unique: `ix_{table}_{column}` — e.g., `ix_push_device_registration_payment_request`
- Unique:     `ux_{table}_{column}` — e.g., `ux_push_device_registration_token`

Foreign key constraints: `fk_{table}_{referenced_table}`.

---

## 5. SQL Changeset Format

Use SQL format for changesets — it's readable, diffable, and works with any database tool.

```sql
-- liquibase formatted sql

-- changeset dev:0002-add-invoice-document-table
CREATE TABLE invoice_document (
    id              UUID        NOT NULL PRIMARY KEY,
    invoice_number  VARCHAR(50) NOT NULL UNIQUE,
    issue_date      DATE        NOT NULL,
    due_date        DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    amount_cents    BIGINT      NOT NULL,
    currency        CHAR(3)     NOT NULL DEFAULT 'EUR',
    buyer_vat_id    VARCHAR(30),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- rollback DROP TABLE invoice_document;

-- changeset dev:0002-add-invoice-document-status-index
CREATE INDEX ix_invoice_document_status ON invoice_document (status);
-- rollback DROP INDEX ix_invoice_document_status;
```

**Rules:**
- Every `changeset` must have a `-- rollback` comment describing how to undo it
- `id` format: `{author}:{sequence}-{description}` — makes it unique in the `DATABASECHANGELOG` table
- Use `TIMESTAMPTZ` (timestamp with timezone) for all timestamp columns — never `TIMESTAMP`
- One logical change per changeset (table creation, index, column add — each gets its own)

---

## 6. Never Modify an Applied Changeset

Once a changeset has been applied to any environment (dev, staging, prod), never edit it.
Liquibase checksums the changeset content and will fail on startup if it detects a change.

**Instead**, add a new changeset:

```sql
-- Wrong: editing 0002 after it has been applied
-- Right: add a new changeset

-- changeset dev:0004-rename-buyer-tax-id-to-vat-id
ALTER TABLE invoice_document RENAME COLUMN buyer_tax_id TO buyer_vat_id;
-- rollback ALTER TABLE invoice_document RENAME COLUMN buyer_vat_id TO buyer_tax_id;
```

---

## 7. Rollback Strategy

Always write rollback instructions in every changeset. They enable:
- Fast recovery from a bad deployment
- Local dev iteration without resetting the database
- CI environment teardown

```bash
# Roll back the last 1 changeset
liquibase rollbackCount 1

# Roll back to a specific tag
liquibase rollback v1.2.3
```

Tag a release before deploying:
```bash
liquibase tag v1.2.3
```

---

## 8. Application Startup Behavior

With Liquibase enabled:
1. On startup, Spring runs all pending changesets in order before the application context
   fully starts
2. If a changeset fails, the application fails to start — schema and app are always in sync
3. The `DATABASECHANGELOG` table tracks what has been applied and when

This replaces the non-deterministic behaviour of `ddl-auto: update`.

---

## 9. Running Liquibase in Tests

For contract tests (which mock the datasource), Liquibase must be disabled:

```yaml
# src/contractTest/resources/application-contract-test.yml
spring:
  liquibase:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: none
```

For integration tests that use a real database (Testcontainers), Liquibase runs normally —
this is the correct behavior since it validates the migration against a real PostgreSQL instance.

---

## References

- Liquibase docs: https://docs.liquibase.com
- Spring Boot Liquibase integration: https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.liquibase
- SQL changelog format: https://docs.liquibase.com/concepts/changelogs/sql-format.html
