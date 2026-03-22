-- liquibase formatted sql

-- changeset aurum:add-icon-to-categories
ALTER TABLE asset_categories ADD COLUMN icon VARCHAR(100);
