--liquibase formatted sql

-- Every constraint below is NOT VALID on purpose. Production and the developer database hold rows
-- that predate the invariant (12 MANUAL rows with payment_amount = 0), and this batch rewrites no
-- row: NOT VALID enforces the rule on every insert and update from now on while leaving the stored
-- data untouched. A later VALIDATE CONSTRAINT follows once those legacy rows are cleaned.
--changeset aurum:add-liability-invariants
ALTER TABLE assets ADD CONSTRAINT chk_assets_payment_amount_non_negative
    CHECK (payment_amount IS NULL OR payment_amount >= 0) NOT VALID;

-- The amount test is written as IS NOT NULL AND > 0 on purpose: a bare `payment_amount > 0` is
-- NULL when the amount is absent, and a CHECK passes on NULL, which would let an AUTOMATIC
-- liability be stored with no amount at all — the exact shape this constraint exists to reject.
ALTER TABLE assets ADD CONSTRAINT chk_assets_automatic_liability_configured
    CHECK (liability_type IS DISTINCT FROM 'AUTOMATIC' OR (payment_frequency IS NOT NULL AND payment_amount IS NOT NULL AND payment_amount > 0)) NOT VALID;

ALTER TABLE assets ADD CONSTRAINT chk_assets_manual_liability_no_amount
    CHECK (liability_type IS DISTINCT FROM 'MANUAL' OR payment_amount IS NULL OR payment_amount <= 0) NOT VALID;

ALTER TABLE assets ADD CONSTRAINT chk_assets_liability_fields_need_type
    CHECK (liability_type IS NOT NULL OR (payment_frequency IS NULL AND payment_amount IS NULL)) NOT VALID;

--rollback ALTER TABLE assets DROP CONSTRAINT IF EXISTS chk_assets_liability_fields_need_type;
--rollback ALTER TABLE assets DROP CONSTRAINT IF EXISTS chk_assets_manual_liability_no_amount;
--rollback ALTER TABLE assets DROP CONSTRAINT IF EXISTS chk_assets_automatic_liability_configured;
--rollback ALTER TABLE assets DROP CONSTRAINT IF EXISTS chk_assets_payment_amount_non_negative;
