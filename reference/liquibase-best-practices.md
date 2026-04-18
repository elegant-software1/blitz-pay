# Liquibase Best Practices

Authoritative reference for database schema migration conventions in this project.
See `CONTRIBUTING.md` for the link to this document.

---

## 1. Dependency (Spring Boot 4)

Spring Boot 4 moved Liquibase support into a dedicated starter. Use the starter —
not the raw `liquibase-core` artifact — so that autoconfiguration is registered correctly.

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-liquibase")
```

> **Why not `org.liquibase:liquibase-core` directly?**
> Spring Boot 4 ships `LiquibaseAutoConfiguration` in a separate autoconfig module
> that is only pulled in via the starter. Adding `liquibase-core` alone leaves the
> autoconfig absent, so Liquibase never runs at startup.

---

## 2. `application.yml` Configuration

```yaml
spring:
  datasource:
    hikari:
      connection-init-sql: "SET search_path TO blitzpay, public"
  jpa:
    hibernate:
      ddl-auto: none        # Liquibase owns the schema; Hibernate must not touch it
    properties:
      hibernate:
        default_schema: blitzpay
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
    default-schema: blitzpay   # working schema for app objects
    liquibase-schema: public   # where DATABASECHANGELOG / DATABASECHANGELOGLOCK live
```

`ddl-auto: none` means Hibernate never creates or modifies tables.
`ddl-auto: validate` is the production alternative — it validates entities against
the schema but still does not modify it.

---

## 3. Changelog File Structure

```
src/main/resources/db/
└── changelog/
    ├── db.changelog-master.yaml              ← include list only, no inline changesets
    ├── 20260417-001-create-blitzpay-schema.sql
    ├── 20260418-001-create-push-tables.sql
    └── YYYYMMDD-NNN-<verb>-<object>.sql      ← future migrations follow this pattern
```

**`db.changelog-master.yaml`** is an ordered include list. Add each new file here
in chronological order. Never put changesets directly in this file.

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/20260417-001-create-blitzpay-schema.sql
  - include:
      file: db/changelog/20260418-001-create-push-tables.sql
```

---

## 4. File and Changeset Naming Convention

### File names

```
YYYYMMDD-NNN-<verb>-<object>.sql
```

| Part | Rule | Example |
|---|---|---|
| `YYYYMMDD` | Date the script is authored | `20260418` |
| `NNN` | Sequential within the same day, zero-padded to 3 digits | `001`, `002` |
| `<verb>` | What is being done | `create`, `add`, `rename`, `drop`, `alter` |
| `<object>` | Target table, column, or index | `push-tables`, `device-registration-index` |

Examples:
```
20260418-001-create-push-tables.sql
20260419-001-add-push-delivery-retry-count.sql
20260419-002-add-push-payment-status-index.sql
20260501-001-rename-payer-ref-to-external-ref.sql
```

### Changeset IDs

Format: `author:YYYYMMDD-NNN-description`

- The `author:id` pair must be globally unique across the entire changelog history.
- Use your Git username as the author — not `dev`.
- Number changesets within a file sequentially when one file contains multiple changesets.

```sql
-- changeset mehdi:20260418-001-push-device-registration
-- changeset mehdi:20260418-002-push-payment-status
-- changeset mehdi:20260418-003-push-processed-webhook-event
-- changeset mehdi:20260418-004-push-delivery-attempt
```

---

## 5. Table and Index Naming Convention

All application objects live in the `blitzpay` schema. Tables are owned by exactly
one Spring Modulith **leaf** module and are prefixed with that module's identifier.

**Format:** `{leaf_module}_{table}`

| Leaf module package | Prefix | Example tables |
|---|---|---|
| `payments.push` | `push_` | `push_device_registration`, `push_payment_status`, `push_delivery_attempt`, `push_processed_webhook_event` |
| `payments.qrpay` | `qrpay_` | `qrpay_request`, `qrpay_session` |
| `payments.truelayer` | `truelayer_` | `truelayer_payment`, `truelayer_webhook_event` |
| `invoice` | `invoice_` | `invoice_document`, `invoice_line_item` |

Rules:
- A table may only be read/written by its owning module.
- Use the **leaf** module prefix, not the parent — `push_` not `payments_`.
- Cross-module access goes through the owning module's `api` surface (named interfaces, events).

**Index naming:**
- Non-unique: `ix_{table}_{column}` → `ix_push_device_registration_payment_request`
- Unique:     `ux_{table}_{column}` → `ux_push_device_registration_token`
- Foreign key: `fk_{table}_{referenced_table}`

---

## 6. SQL Changeset Format

Use SQL format (`.sql` extension). It is readable, diffable, and works with any database tool.

```sql
-- liquibase formatted sql

-- changeset mehdi:20260419-001-create-qrpay-request
CREATE TABLE blitzpay.qrpay_request (
    id              UUID         NOT NULL,
    payer_ref       VARCHAR(128) NOT NULL,
    amount_cents    BIGINT       NOT NULL,
    currency        CHAR(3)      NOT NULL DEFAULT 'EUR',
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_qrpay_request PRIMARY KEY (id)
);
CREATE INDEX ix_qrpay_request_payer_ref ON blitzpay.qrpay_request (payer_ref);
-- rollback DROP TABLE blitzpay.qrpay_request;

-- changeset mehdi:20260419-002-add-qrpay-request-status-index
CREATE INDEX ix_qrpay_request_status ON blitzpay.qrpay_request (status);
-- rollback DROP INDEX blitzpay.ix_qrpay_request_status;
```

**Rules:**
- Always schema-qualify DDL: `blitzpay.table_name`, not just `table_name`.
- Every changeset must have a `-- rollback` directive.
- Use `TIMESTAMPTZ` — never plain `TIMESTAMP`.
- One logical change per changeset. A `CREATE TABLE` and its indexes may be in the same
  changeset; adding a column later is always a separate changeset.
- Use `runInTransaction:false` only for DDL that PostgreSQL cannot run in a transaction
  (e.g., `CREATE SCHEMA`, `CREATE INDEX CONCURRENTLY`).

---

## 7. Never Modify an Applied Changeset

Once a changeset has been applied to any environment, never edit its SQL.
Liquibase checksums the content and will refuse to start if it detects a mismatch.

To fix or extend something, add a new changeset:

```sql
-- Wrong: editing 20260418-001 after it has been applied to staging
-- Right:

-- changeset mehdi:20260501-001-rename-payer-ref-to-external-ref
ALTER TABLE blitzpay.push_device_registration
    RENAME COLUMN payer_ref TO external_ref;
-- rollback ALTER TABLE blitzpay.push_device_registration RENAME COLUMN external_ref TO payer_ref;
```

---

## 8. Rollback Strategy

Always write rollback instructions. They enable fast recovery and CI teardown.

```bash
# Roll back the last N changesets
liquibase rollbackCount 1

# Roll back to a tagged release
liquibase rollback v1.2.3
```

Tag before deploying:
```bash
liquibase tag v1.2.3
```

---

## 9. Tests

**Contract tests** mock the datasource — disable Liquibase:

```yaml
# src/contractTest/resources/application-contract-test.yml
spring:
  liquibase:
    enabled: false
```

**Integration tests** (Testcontainers) run Liquibase normally — this is the correct
behaviour since it validates migrations against a real PostgreSQL instance.

---

## References

- Liquibase docs: https://docs.liquibase.com
- Spring Boot Liquibase integration: https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.liquibase
- SQL changelog format: https://docs.liquibase.com/concepts/changelogs/sql-format.html
