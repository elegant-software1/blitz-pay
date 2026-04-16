-- liquibase formatted sql

-- =========================================================================
-- BASELINE — current schema as created historically by Hibernate `ddl-auto: update`.
--
-- TODO: replace this placeholder with an actual dump from staging:
--   pg_dump --schema-only --no-owner --no-acl --schema=blitzpay \
--     -U postgres quickpay_db > baseline.sql
--
-- Place each CREATE TABLE / CREATE INDEX under its own `-- changeset` line.
-- Every changeset MUST have a `-- rollback` directive.
--
-- This file is intentionally empty until a baseline dump is available; the
-- master changelog still references it so the order is reserved.
-- =========================================================================

-- changeset dev:0001-baseline runOnChange:false
-- rollback
SELECT 1;
