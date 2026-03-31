--liquibase formatted sql

--changeset aurum:add-cash-flows
CREATE TABLE cash_flows (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  year INT NOT NULL,
  month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
  earned DECIMAL(19, 4) NOT NULL DEFAULT 0,
  spent DECIMAL(19, 4) NOT NULL DEFAULT 0,
  CONSTRAINT uq_cash_flows_user_year_month UNIQUE (user_id, year, month),
  CONSTRAINT chk_cash_flows_earned_positive CHECK (earned >= 0),
  CONSTRAINT chk_cash_flows_spent_positive CHECK (spent >= 0)
);

CREATE INDEX idx_cash_flows_user_year ON cash_flows (user_id, year);

--rollback DROP TABLE cash_flows;
