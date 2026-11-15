--liquibase formatted sql

-- A snapshot records the currency it was written in. Until now every read converted through
-- assets.original_currency, so changing an asset's currency restated its whole history; from this
-- this one the row owns its currency and the asset's current one only applies to new writes.
-- The backfill is exact only where the asset currency never changed before this deploy (true in
-- production); past changes are not reconstructable from the data we kept.
--changeset aurum:add-snapshot-currency
ALTER TABLE snapshots ADD COLUMN currency VARCHAR(3);

UPDATE snapshots s
SET currency = a.original_currency
FROM assets a
WHERE a.id = s.asset_id;

ALTER TABLE snapshots ALTER COLUMN currency SET NOT NULL;

--rollback ALTER TABLE snapshots DROP COLUMN IF EXISTS currency;
