--liquibase formatted sql

--changeset mario:add-picture-to-users
ALTER TABLE users ADD COLUMN picture TEXT;
