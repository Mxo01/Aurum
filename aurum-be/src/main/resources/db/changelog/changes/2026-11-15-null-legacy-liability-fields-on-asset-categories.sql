--liquibase formatted sql

-- Why the data was wrong: a legacy form wrote liability_type = 'MANUAL' with payment_amount = 0
-- onto ordinary assets, so twelve genuine holdings (bank accounts, physical assets, a retirement
-- account, stocks, vehicles) carried liability fields they never had. Batch 4 (Aurum#67, PR #76)
-- made liability_type the discriminator, so those twelve rows (real assets in ASSET categories)
-- were subtracted from net worth instead of added.
-- The three fields are meaningless on an ASSET-categorised row, so they are cleared rather than
-- corrected to some other liability shape.
--
-- Scope: only rows whose category is an ASSET, and only rows still carrying at least one of the
-- three fields. LIABILITY-categorised rows are never a target of this UPDATE (the category filter
-- runs first, and the category table is only read), and the "at least one non-null" guard makes a
-- second run update zero rows.
--
-- Clearing all three together cannot trip the NOT VALID checks on assets: with no liability_type,
-- chk_assets_liability_fields_need_type only requires payment_frequency and payment_amount to be
-- absent too, and the AUTOMATIC/MANUAL amount rules bind only rows that still declare a type.
--changeset aurum:null-legacy-liability-fields-on-asset-categorised-assets
UPDATE assets a
SET liability_type = NULL,
    payment_frequency = NULL,
    payment_amount = NULL
WHERE a.category_id IN (SELECT c.id FROM asset_categories c WHERE c.type = 'ASSET')
  AND (
    a.liability_type IS NOT NULL
    OR a.payment_frequency IS NOT NULL
    OR a.payment_amount IS NOT NULL
  );

--rollback (not reversible - data migration)
