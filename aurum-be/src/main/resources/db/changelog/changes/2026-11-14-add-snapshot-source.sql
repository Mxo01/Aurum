--liquibase formatted sql

--changeset aurum:add-snapshot-source
ALTER TABLE snapshots
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'USER';

ALTER TABLE snapshots
    ADD CONSTRAINT chk_snapshots_source CHECK (source IN ('USER', 'SCHEDULER'));

-- One scheduler row per asset and date: the cron's idempotency guard at the database level.
CREATE UNIQUE INDEX uq_snapshots_scheduler_asset_date
    ON snapshots (asset_id, reference_date)
    WHERE source = 'SCHEDULER';

--rollback DROP INDEX IF EXISTS uq_snapshots_scheduler_asset_date;
--rollback ALTER TABLE snapshots DROP CONSTRAINT IF EXISTS chk_snapshots_source;
--rollback ALTER TABLE snapshots DROP COLUMN IF EXISTS source;
