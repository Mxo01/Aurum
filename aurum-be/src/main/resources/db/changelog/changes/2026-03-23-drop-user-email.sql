--liquibase formatted sql

--changeset aurum:drop-user-email
-- Email is available on every request from the JWT claim and never needs to be
-- persisted. Dropping it eliminates the only column that links a readable
-- identity to financial records.
ALTER TABLE users DROP COLUMN email;

--rollback ALTER TABLE users ADD COLUMN email VARCHAR(255);
