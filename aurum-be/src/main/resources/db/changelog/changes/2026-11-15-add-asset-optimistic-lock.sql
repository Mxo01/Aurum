--liquibase formatted sql

-- The daily liability batch loads an asset, mutates its schedule and writes the whole row back, so
-- a user edit that commits in between was silently overwritten for every column the batch did not
-- touch. The version column makes the second writer miss its UPDATE and fail instead of winning by
-- accident. The DEFAULT is kept so raw SQL seeding and rollback scripts stay insertable without
-- naming the column.
--changeset aurum:add-asset-optimistic-lock
ALTER TABLE assets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--rollback ALTER TABLE assets DROP COLUMN IF EXISTS version;
