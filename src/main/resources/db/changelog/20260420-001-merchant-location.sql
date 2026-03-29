-- liquibase formatted sql

-- changeset mehdi:20260420-010-merchant-location
ALTER TABLE blitzpay.merchant_applications
    ADD COLUMN latitude           DOUBLE PRECISION,
    ADD COLUMN longitude          DOUBLE PRECISION,
    ADD COLUMN geofence_radius_m  INTEGER,
    ADD COLUMN google_place_id    VARCHAR(255);

CREATE INDEX ix_merchant_applications_location
    ON blitzpay.merchant_applications (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
-- rollback DROP INDEX blitzpay.ix_merchant_applications_location;
--         ALTER TABLE blitzpay.merchant_applications DROP COLUMN google_place_id;
--         ALTER TABLE blitzpay.merchant_applications DROP COLUMN geofence_radius_m;
--         ALTER TABLE blitzpay.merchant_applications DROP COLUMN longitude;
--         ALTER TABLE blitzpay.merchant_applications DROP COLUMN latitude;
