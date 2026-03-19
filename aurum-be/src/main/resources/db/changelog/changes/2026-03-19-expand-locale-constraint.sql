--liquibase formatted sql

--changeset aurum:2026-03-19-expand-locale-constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_locale_check;
ALTER TABLE users ADD CONSTRAINT users_locale_check CHECK (locale BETWEEN 0 AND 5);
--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS users_locale_check;
--rollback ALTER TABLE users ADD CONSTRAINT users_locale_check CHECK (locale BETWEEN 0 AND 3);
