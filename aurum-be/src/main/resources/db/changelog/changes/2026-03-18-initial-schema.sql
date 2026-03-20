--liquibase formatted sql

--changeset aurum:initial-schema
CREATE TABLE users (
    id UUID PRIMARY KEY,
    jwt_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    currency INTEGER NOT NULL
);

CREATE TABLE asset_categories (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES asset_categories(id) ON DELETE CASCADE,
    original_currency INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE snapshots (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    reference_date DATE NOT NULL,
    amount_original_currency DECIMAL(19, 4) NOT NULL,
    exchange_rate_to_base DECIMAL(19, 6) NOT NULL DEFAULT 1.000000
);

CREATE TABLE targets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- no @OnDelete on entity, added at DB level for integrity
    name VARCHAR(255) NOT NULL,
    target_amount DECIMAL(19, 4) NOT NULL,
    current_amount DECIMAL(19, 4),
    type VARCHAR(255) NOT NULL DEFAULT 'MANUAL',
    deadline DATE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE
);

--rollback DROP TABLE IF EXISTS targets;
--rollback DROP TABLE IF EXISTS snapshots;
--rollback DROP TABLE IF EXISTS assets;
--rollback DROP TABLE IF EXISTS asset_categories;
--rollback DROP TABLE IF EXISTS users;
