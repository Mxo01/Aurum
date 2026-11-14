--liquibase formatted sql

--changeset aurum:add-index-assets-liability-type
CREATE INDEX IF NOT EXISTS idx_assets_liability_type
    ON assets (liability_type);

--rollback DROP INDEX IF EXISTS idx_assets_liability_type;
