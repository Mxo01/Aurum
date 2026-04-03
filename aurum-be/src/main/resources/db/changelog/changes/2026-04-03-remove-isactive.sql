-- liquibase formatted sql

-- changeset aurum:remove-isactive
DROP TABLE IF EXISTS asset_status_log;
DROP INDEX IF EXISTS idx_assets_user_id_active;
ALTER TABLE assets DROP COLUMN IF EXISTS is_active;
