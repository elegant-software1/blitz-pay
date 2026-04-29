-- liquibase formatted sql

-- changeset mehdi:20260429-001-product-code-support
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN product_code BIGINT;

CREATE UNIQUE INDEX uq_merchant_products_branch_product_code
    ON blitzpay.merchant_products (merchant_branch_id, product_code)
    WHERE product_code IS NOT NULL;

CREATE INDEX ix_merchant_products_branch_product_code_desc
    ON blitzpay.merchant_products (merchant_branch_id, product_code DESC)
    WHERE product_code IS NOT NULL;

-- rollback DROP INDEX IF EXISTS blitzpay.ix_merchant_products_branch_product_code_desc;
-- rollback DROP INDEX IF EXISTS blitzpay.uq_merchant_products_branch_product_code;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN product_code;
