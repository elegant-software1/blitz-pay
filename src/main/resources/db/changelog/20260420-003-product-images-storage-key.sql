-- liquibase formatted sql

-- changeset mehdi:20260420-014-product-images-rename-to-storage-key
ALTER TABLE blitzpay.merchant_product_images
    RENAME COLUMN image_url TO storage_key;
-- rollback ALTER TABLE blitzpay.merchant_product_images RENAME COLUMN storage_key TO image_url;
