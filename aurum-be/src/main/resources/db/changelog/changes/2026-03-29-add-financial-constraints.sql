--liquibase formatted sql

--changeset aurum:add-financial-constraints
ALTER TABLE snapshots ADD CONSTRAINT chk_exchange_rate_positive CHECK (exchange_rate_to_base > 0);
ALTER TABLE snapshots ADD CONSTRAINT uq_snapshot_asset_date UNIQUE (asset_id, reference_date);
ALTER TABLE targets ADD CONSTRAINT chk_target_amount_positive CHECK (target_amount > 0);

--rollback ALTER TABLE targets DROP CONSTRAINT IF EXISTS chk_target_amount_positive;
--rollback ALTER TABLE snapshots DROP CONSTRAINT IF EXISTS uq_snapshot_asset_date;
--rollback ALTER TABLE snapshots DROP CONSTRAINT IF EXISTS chk_exchange_rate_positive;
