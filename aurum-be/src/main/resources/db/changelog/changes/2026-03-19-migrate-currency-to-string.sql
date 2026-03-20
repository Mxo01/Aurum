--liquibase formatted sql

--changeset aurum:2026-03-19-migrate-currency-to-string
-- users.currency: 0=EUR, 1=USD → VARCHAR
ALTER TABLE users ADD COLUMN currency_str VARCHAR(3);
UPDATE users SET currency_str = CASE currency WHEN 0 THEN 'EUR' WHEN 1 THEN 'USD' ELSE 'EUR' END;
ALTER TABLE users DROP COLUMN currency;
ALTER TABLE users RENAME COLUMN currency_str TO currency;
ALTER TABLE users ALTER COLUMN currency SET NOT NULL;

-- assets.original_currency: 0=EUR, 1=USD → VARCHAR
ALTER TABLE assets ADD COLUMN original_currency_str VARCHAR(3);
UPDATE assets SET original_currency_str = CASE original_currency WHEN 0 THEN 'EUR' WHEN 1 THEN 'USD' ELSE 'EUR' END;
ALTER TABLE assets DROP COLUMN original_currency;
ALTER TABLE assets RENAME COLUMN original_currency_str TO original_currency;
ALTER TABLE assets ALTER COLUMN original_currency SET NOT NULL;
--rollback (not reversible - data migration)
