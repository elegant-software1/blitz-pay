-- liquibase formatted sql

-- changeset mehdi:20260417-001-create-blitzpay-schema runInTransaction:false
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'blitzpay'
CREATE SCHEMA IF NOT EXISTS blitzpay;
-- rollback DROP SCHEMA blitzpay;
