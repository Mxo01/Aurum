--liquibase formatted sql

-- Two writers on the same month row must not silently overwrite each other: the version column
-- makes the second UPDATE miss and fail instead. The DEFAULT is kept so raw SQL seeding and
-- rollback scripts stay insertable without naming the column.
--changeset aurum:add-snapshot-optimistic-lock
ALTER TABLE snapshots ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--rollback ALTER TABLE snapshots DROP COLUMN IF EXISTS version;
