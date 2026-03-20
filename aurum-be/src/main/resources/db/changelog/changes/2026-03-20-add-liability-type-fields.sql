--liquibase formatted sql

--changeset aurum:add-liability-type-fields
ALTER TABLE assets
    ADD COLUMN liability_type VARCHAR(20),
    ADD COLUMN payment_frequency VARCHAR(20),
    ADD COLUMN payment_amount DECIMAL(19, 4);
