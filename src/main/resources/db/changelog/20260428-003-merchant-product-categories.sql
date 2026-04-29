-- liquibase formatted sql

-- changeset mehdi:20260428-003-merchant-product-categories
CREATE TABLE blitzpay.merchant_product_categories (
    id                      UUID         NOT NULL,
    merchant_application_id UUID         NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_merchant_product_categories PRIMARY KEY (id),
    CONSTRAINT fk_merchant_product_categories_application
        FOREIGN KEY (merchant_application_id)
        REFERENCES blitzpay.merchant_applications (id),
    CONSTRAINT chk_merchant_product_category_name_length CHECK (char_length(name) <= 100)
);
CREATE UNIQUE INDEX uq_merchant_product_category_name
    ON blitzpay.merchant_product_categories (merchant_application_id, lower(name));
CREATE INDEX ix_merchant_product_categories_merchant
    ON blitzpay.merchant_product_categories (merchant_application_id);
-- rollback DROP TABLE blitzpay.merchant_product_categories;

-- changeset mehdi:20260428-004-merchant-product-categories-rls
ALTER TABLE blitzpay.merchant_product_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE blitzpay.merchant_product_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY merchant_tenant_isolation
    ON blitzpay.merchant_product_categories
    USING (
        merchant_application_id = NULLIF(current_setting('app.current_merchant_id', true), '')::uuid
    );
-- rollback DROP POLICY merchant_tenant_isolation ON blitzpay.merchant_product_categories;
-- rollback ALTER TABLE blitzpay.merchant_product_categories DISABLE ROW LEVEL SECURITY;
