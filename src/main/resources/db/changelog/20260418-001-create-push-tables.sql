-- liquibase formatted sql

-- changeset mehdi:20260418-001-push-device-registration
CREATE TABLE blitzpay.push_device_registration (
    id                  UUID        NOT NULL,
    payment_request_id  UUID,
    payer_ref           VARCHAR(128),
    expo_push_token     VARCHAR(256) NOT NULL,
    platform            VARCHAR(16),
    created_at          TIMESTAMPTZ  NOT NULL,
    last_seen_at        TIMESTAMPTZ  NOT NULL,
    invalid             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_push_device_registration PRIMARY KEY (id)
);
CREATE UNIQUE INDEX ux_push_device_registration_token
    ON blitzpay.push_device_registration (expo_push_token);
CREATE INDEX ix_push_device_registration_payment_request
    ON blitzpay.push_device_registration (payment_request_id);
CREATE INDEX ix_push_device_registration_payer_ref
    ON blitzpay.push_device_registration (payer_ref);
-- rollback DROP TABLE blitzpay.push_device_registration;

-- changeset mehdi:20260418-002-push-payment-status
CREATE TABLE blitzpay.push_payment_status (
    payment_request_id  UUID        NOT NULL,
    current_status      VARCHAR(32)  NOT NULL,
    last_event_id       VARCHAR(128),
    last_event_at       TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_push_payment_status PRIMARY KEY (payment_request_id)
);
-- rollback DROP TABLE blitzpay.push_payment_status;

-- changeset mehdi:20260418-003-push-processed-webhook-event
CREATE TABLE blitzpay.push_processed_webhook_event (
    event_id        VARCHAR(128) NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_push_processed_webhook_event PRIMARY KEY (event_id)
);
-- rollback DROP TABLE blitzpay.push_processed_webhook_event;

-- changeset mehdi:20260418-004-push-delivery-attempt
CREATE TABLE blitzpay.push_delivery_attempt (
    id                  UUID         NOT NULL,
    payment_request_id  UUID         NOT NULL,
    expo_push_token     VARCHAR(256) NOT NULL,
    status_code         VARCHAR(32)  NOT NULL,
    ticket_id           VARCHAR(128),
    outcome             VARCHAR(16)  NOT NULL,
    receipt_outcome     VARCHAR(16),
    error_code          VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_push_delivery_attempt PRIMARY KEY (id)
);
CREATE INDEX ix_push_delivery_payment_request
    ON blitzpay.push_delivery_attempt (payment_request_id);
CREATE INDEX ix_push_delivery_token
    ON blitzpay.push_delivery_attempt (expo_push_token);
CREATE INDEX ix_push_delivery_ticket
    ON blitzpay.push_delivery_attempt (ticket_id);
-- rollback DROP TABLE blitzpay.push_delivery_attempt;
