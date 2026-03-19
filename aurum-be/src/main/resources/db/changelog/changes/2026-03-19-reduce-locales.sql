--liquibase formatted sql

--changeset aurum:2026-03-19-reduce-locales
-- Drop constraint before remapping
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_locale_check;
-- Map removed locales (ZH_CN=1, EN_GB=2, DE=4) to EN_US=0
UPDATE users SET locale = 0 WHERE locale IN (1, 2, 4);
-- Remap FR: 3 -> 1
UPDATE users SET locale = 1 WHERE locale = 3;
-- Remap IT: 5 -> 2
UPDATE users SET locale = 2 WHERE locale = 5;
-- Restore constraint for the 3 remaining locales
ALTER TABLE users ADD CONSTRAINT users_locale_check CHECK (locale BETWEEN 0 AND 2);
--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS users_locale_check;
--rollback ALTER TABLE users ADD CONSTRAINT users_locale_check CHECK (locale BETWEEN 0 AND 5);
