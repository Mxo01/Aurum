--liquibase formatted sql

--changeset aurum:2026-03-19-add-locale-to-users
ALTER TABLE users ADD COLUMN locale smallint NOT NULL DEFAULT 0 CHECK (locale BETWEEN 0 AND 3);
--rollback ALTER TABLE users DROP COLUMN locale;
