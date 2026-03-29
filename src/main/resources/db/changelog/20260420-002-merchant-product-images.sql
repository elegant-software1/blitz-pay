-- liquibase formatted sql

-- changeset mehdi:20260420-011-merchant-product-images
CREATE TABLE blitzpay.merchant_product_images (
    product_id    UUID          NOT NULL,
    image_url     VARCHAR(2048) NOT NULL,
    display_order INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES blitzpay.merchant_products (id)
);
CREATE INDEX ix_product_images_product ON blitzpay.merchant_product_images (product_id);
-- rollback DROP TABLE blitzpay.merchant_product_images;

-- changeset mehdi:20260420-012-migrate-product-image-url
INSERT INTO blitzpay.merchant_product_images (product_id, image_url, display_order)
SELECT id, image_url, 0
FROM blitzpay.merchant_products
WHERE image_url IS NOT NULL;
-- rollback DELETE FROM blitzpay.merchant_product_images WHERE display_order = 0;

-- changeset mehdi:20260420-013-drop-product-image-url
ALTER TABLE blitzpay.merchant_products DROP COLUMN image_url;
-- rollback ALTER TABLE blitzpay.merchant_products ADD COLUMN image_url VARCHAR(2048);
