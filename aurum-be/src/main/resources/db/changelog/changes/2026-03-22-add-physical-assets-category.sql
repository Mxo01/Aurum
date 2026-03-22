-- liquibase formatted sql

-- changeset aurum:add-physical-assets-category
INSERT INTO asset_categories (id, user_id, name, type, icon) VALUES
    ('a0000000-0000-0000-0000-000000000016', NULL, 'Physical Assets', 'ASSET', 'pi-warehouse');
