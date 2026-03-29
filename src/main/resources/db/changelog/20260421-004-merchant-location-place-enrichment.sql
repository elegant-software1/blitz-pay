-- liquibase formatted sql

-- changeset mehdi:20260421-006-merchant-location-place-enrichment
ALTER TABLE blitzpay.merchant_applications
    ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(255),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS country VARCHAR(2),
    ADD COLUMN IF NOT EXISTS place_formatted_address VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS place_rating DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS place_review_count INTEGER,
    ADD COLUMN IF NOT EXISTS place_enrichment_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS place_enrichment_error VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS place_enriched_at TIMESTAMPTZ;

ALTER TABLE blitzpay.merchant_branches
    ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(255),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS country VARCHAR(2),
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS geofence_radius_m INTEGER,
    ADD COLUMN IF NOT EXISTS google_place_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS place_formatted_address VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS place_rating DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS place_review_count INTEGER,
    ADD COLUMN IF NOT EXISTS place_enrichment_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS place_enrichment_error VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS place_enriched_at TIMESTAMPTZ;
-- rollback ALTER TABLE blitzpay.merchant_branches
-- rollback     DROP COLUMN IF EXISTS place_enriched_at,
-- rollback     DROP COLUMN IF EXISTS place_enrichment_error,
-- rollback     DROP COLUMN IF EXISTS place_enrichment_status,
-- rollback     DROP COLUMN IF EXISTS place_review_count,
-- rollback     DROP COLUMN IF EXISTS place_rating,
-- rollback     DROP COLUMN IF EXISTS place_formatted_address,
-- rollback     DROP COLUMN IF EXISTS google_place_id,
-- rollback     DROP COLUMN IF EXISTS geofence_radius_m,
-- rollback     DROP COLUMN IF EXISTS longitude,
-- rollback     DROP COLUMN IF EXISTS latitude,
-- rollback     DROP COLUMN IF EXISTS country,
-- rollback     DROP COLUMN IF EXISTS postal_code,
-- rollback     DROP COLUMN IF EXISTS city,
-- rollback     DROP COLUMN IF EXISTS address_line2,
-- rollback     DROP COLUMN IF EXISTS address_line1;
-- rollback ALTER TABLE blitzpay.merchant_applications
-- rollback     DROP COLUMN IF EXISTS place_enriched_at,
-- rollback     DROP COLUMN IF EXISTS place_enrichment_error,
-- rollback     DROP COLUMN IF EXISTS place_enrichment_status,
-- rollback     DROP COLUMN IF EXISTS place_review_count,
-- rollback     DROP COLUMN IF EXISTS place_rating,
-- rollback     DROP COLUMN IF EXISTS place_formatted_address,
-- rollback     DROP COLUMN IF EXISTS country,
-- rollback     DROP COLUMN IF EXISTS postal_code,
-- rollback     DROP COLUMN IF EXISTS city,
-- rollback     DROP COLUMN IF EXISTS address_line2,
-- rollback     DROP COLUMN IF EXISTS address_line1;
