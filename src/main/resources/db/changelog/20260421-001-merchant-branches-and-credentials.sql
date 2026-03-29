-- liquibase formatted sql

-- changeset mehdi:20260421-001-merchant-application-credentials
ALTER TABLE blitzpay.merchant_applications
    ADD COLUMN stripe_secret_key       VARCHAR(512),
    ADD COLUMN stripe_publishable_key  VARCHAR(512),
    ADD COLUMN braintree_merchant_id   VARCHAR(255),
    ADD COLUMN braintree_public_key    VARCHAR(255),
    ADD COLUMN braintree_private_key   VARCHAR(512),
    ADD COLUMN braintree_environment   VARCHAR(64);
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN braintree_environment;
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN braintree_private_key;
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN braintree_public_key;
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN braintree_merchant_id;
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN stripe_publishable_key;
-- rollback ALTER TABLE blitzpay.merchant_applications DROP COLUMN stripe_secret_key;

-- changeset mehdi:20260421-002-merchant-branches
CREATE TABLE blitzpay.merchant_branches (
    id                      UUID         NOT NULL,
    merchant_application_id UUID         NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    stripe_secret_key       VARCHAR(512),
    stripe_publishable_key  VARCHAR(512),
    braintree_merchant_id   VARCHAR(255),
    braintree_public_key    VARCHAR(255),
    braintree_private_key   VARCHAR(512),
    braintree_environment   VARCHAR(64),
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    geofence_radius_m       INTEGER,
    google_place_id         VARCHAR(255),
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_merchant_branches PRIMARY KEY (id),
    CONSTRAINT fk_merchant_branches_application FOREIGN KEY (merchant_application_id) REFERENCES blitzpay.merchant_applications (id)
);
CREATE INDEX idx_merchant_branches_merchant_application_id ON blitzpay.merchant_branches (merchant_application_id);
-- rollback DROP TABLE blitzpay.merchant_branches;

-- changeset mehdi:20260421-003-merchant-products-branch-link
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN merchant_branch_id UUID,
    ADD CONSTRAINT fk_merchant_products_branch
        FOREIGN KEY (merchant_branch_id)
        REFERENCES blitzpay.merchant_branches (id);
-- rollback ALTER TABLE blitzpay.merchant_products DROP CONSTRAINT fk_merchant_products_branch;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN merchant_branch_id;
