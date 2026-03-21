--liquibase formatted sql

--changeset aurum:add-performance-indexes
CREATE INDEX idx_snapshots_asset_id ON snapshots(asset_id);
CREATE INDEX idx_snapshots_asset_id_date ON snapshots(asset_id, reference_date);
CREATE INDEX idx_assets_user_id ON assets(user_id);
CREATE INDEX idx_assets_user_id_active ON assets(user_id, is_active);
CREATE INDEX idx_targets_user_id ON targets(user_id);
CREATE INDEX idx_categories_user_id ON asset_categories(user_id);

--rollback DROP INDEX IF EXISTS idx_categories_user_id;
--rollback DROP INDEX IF EXISTS idx_targets_user_id;
--rollback DROP INDEX IF EXISTS idx_assets_user_id_active;
--rollback DROP INDEX IF EXISTS idx_assets_user_id;
--rollback DROP INDEX IF EXISTS idx_snapshots_asset_id_date;
--rollback DROP INDEX IF EXISTS idx_snapshots_asset_id;
