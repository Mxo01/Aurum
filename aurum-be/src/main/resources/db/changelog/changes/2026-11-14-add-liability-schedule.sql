--liquibase formatted sql

--changeset aurum:add-liability-schedule
ALTER TABLE assets
    ADD COLUMN next_due_date DATE,
    ADD COLUMN payment_anchor_day SMALLINT,
    ADD COLUMN settled_at TIMESTAMP;

ALTER TABLE assets
    ADD CONSTRAINT chk_assets_payment_anchor_day CHECK (payment_anchor_day BETWEEN 1 AND 31);

--rollback ALTER TABLE assets DROP CONSTRAINT IF EXISTS chk_assets_payment_anchor_day;
--rollback ALTER TABLE assets DROP COLUMN IF EXISTS settled_at;
--rollback ALTER TABLE assets DROP COLUMN IF EXISTS payment_anchor_day;
--rollback ALTER TABLE assets DROP COLUMN IF EXISTS next_due_date;
