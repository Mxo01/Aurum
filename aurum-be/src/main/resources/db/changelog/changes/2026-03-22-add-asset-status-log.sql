-- liquibase formatted sql

-- changeset aurum:add-asset-status-log
CREATE TABLE asset_status_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    changed_at DATE NOT NULL,
    is_active BOOLEAN NOT NULL
);

CREATE INDEX idx_asset_status_log_asset_id ON asset_status_log(asset_id);

-- Seed: record initial "active" entry for every existing asset at its creation date
INSERT INTO asset_status_log (asset_id, changed_at, is_active)
SELECT id, DATE(created_at), true
FROM assets;

-- Seed: record a "false" entry today for assets that are currently inactive
INSERT INTO asset_status_log (asset_id, changed_at, is_active)
SELECT id, CURRENT_DATE, false
FROM assets
WHERE is_active = false;
