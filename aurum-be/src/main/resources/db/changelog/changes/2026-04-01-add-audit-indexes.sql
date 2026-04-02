--liquibase formatted sql

--changeset audit:add-composite-index-snapshots-asset-active
CREATE INDEX IF NOT EXISTS idx_snapshots_asset_id_reference_date
    ON snapshots (asset_id, reference_date DESC);

--changeset audit:add-index-targets-is-completed
CREATE INDEX IF NOT EXISTS idx_targets_is_completed
    ON targets (is_completed);
