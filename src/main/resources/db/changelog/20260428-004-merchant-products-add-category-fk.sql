-- liquibase formatted sql

-- changeset mehdi:20260428-005-merchant-products-add-category-fk
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN product_category_id UUID,
    ADD CONSTRAINT fk_merchant_products_category
        FOREIGN KEY (product_category_id)
        REFERENCES blitzpay.merchant_product_categories (id);
CREATE INDEX ix_merchant_products_category
    ON blitzpay.merchant_products (product_category_id)
    WHERE product_category_id IS NOT NULL;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN product_category_id;
