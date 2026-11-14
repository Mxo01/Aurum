--liquibase formatted sql

-- The 2026-03-29 constraint made a user valuation and a scheduled payment on the same date
-- impossible, which forced the scheduler to overwrite the user's row. Uniqueness is now scoped by
-- source: a scheduled payment appends its own row, and the user's row of that date is untouched.
-- No row is written, moved or deleted by this changeset.
-- Rollback recreates the old asset/date constraint and fails if by then a user row and a scheduler
-- row share the same asset and date, because that pair is legal under this changeset.
--changeset aurum:scope-snapshot-uniqueness-by-source
ALTER TABLE snapshots DROP CONSTRAINT IF EXISTS uq_snapshot_asset_date;

CREATE UNIQUE INDEX uq_snapshots_user_asset_date
    ON snapshots (asset_id, reference_date)
    WHERE source = 'USER';

--rollback DROP INDEX IF EXISTS uq_snapshots_user_asset_date;
--rollback ALTER TABLE snapshots ADD CONSTRAINT uq_snapshot_asset_date UNIQUE (asset_id, reference_date);
