-- liquibase formatted sql

-- changeset mehdi:20260421-005-product-description-and-image-storage-key
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN IF NOT EXISTS description VARCHAR(2000);

ALTER TABLE blitzpay.merchant_products
    RENAME COLUMN image_url TO image_storage_key;
-- rollback ALTER TABLE blitzpay.merchant_products RENAME COLUMN image_storage_key TO image_url;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN IF EXISTS description;
