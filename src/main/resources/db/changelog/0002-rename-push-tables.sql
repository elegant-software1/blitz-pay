-- liquibase formatted sql

-- =========================================================================
-- Rename push-module tables to the leaf-module prefix convention.
-- See CONSTITUTION.md → Persistence and Schema.
--
-- Applied only on environments that carry the legacy unprefixed tables
-- from the `ddl-auto: update` era. Fresh databases skip these changesets
-- via the `tableExists` preconditions.
-- =========================================================================

-- changeset dev:0002-rename-processed-webhook-event
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.tables WHERE table_schema = 'blitzpay' AND table_name = 'processed_webhook_event'
ALTER TABLE blitzpay.processed_webhook_event RENAME TO push_processed_webhook_event;
-- rollback ALTER TABLE blitzpay.push_processed_webhook_event RENAME TO processed_webhook_event;

-- changeset dev:0002-rename-device-registration
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.tables WHERE table_schema = 'blitzpay' AND table_name = 'device_registration'
ALTER TABLE blitzpay.device_registration RENAME TO push_device_registration;
ALTER INDEX IF EXISTS blitzpay.ux_device_registration_token RENAME TO ux_push_device_registration_token;
ALTER INDEX IF EXISTS blitzpay.ix_device_registration_payment_request RENAME TO ix_push_device_registration_payment_request;
ALTER INDEX IF EXISTS blitzpay.ix_device_registration_payer_ref RENAME TO ix_push_device_registration_payer_ref;
-- rollback ALTER INDEX IF EXISTS blitzpay.ix_push_device_registration_payer_ref RENAME TO ix_device_registration_payer_ref;
-- rollback ALTER INDEX IF EXISTS blitzpay.ix_push_device_registration_payment_request RENAME TO ix_device_registration_payment_request;
-- rollback ALTER INDEX IF EXISTS blitzpay.ux_push_device_registration_token RENAME TO ux_device_registration_token;
-- rollback ALTER TABLE blitzpay.push_device_registration RENAME TO device_registration;

-- changeset dev:0002-rename-payment-status
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.tables WHERE table_schema = 'blitzpay' AND table_name = 'payment_status'
ALTER TABLE blitzpay.payment_status RENAME TO push_payment_status;
-- rollback ALTER TABLE blitzpay.push_payment_status RENAME TO payment_status;
