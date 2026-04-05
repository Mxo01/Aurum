-- liquibase formatted sql

-- changeset mario:2026-04-05-add-name-length-constraints
ALTER TABLE assets ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE asset_categories ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE asset_categories ALTER COLUMN icon TYPE VARCHAR(50);
ALTER TABLE targets ALTER COLUMN name TYPE VARCHAR(100);
