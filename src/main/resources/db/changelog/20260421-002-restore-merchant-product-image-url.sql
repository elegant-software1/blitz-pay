-- liquibase formatted sql

-- changeset mehdi:20260421-004-restore-merchant-product-image-url
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);

UPDATE blitzpay.merchant_products product
SET image_url = image.storage_key
FROM (
    SELECT DISTINCT ON (product_id)
        product_id,
        storage_key
    FROM blitzpay.merchant_product_images
    ORDER BY product_id, display_order
) image
WHERE product.id = image.product_id
  AND product.image_url IS NULL;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN IF EXISTS image_url;
