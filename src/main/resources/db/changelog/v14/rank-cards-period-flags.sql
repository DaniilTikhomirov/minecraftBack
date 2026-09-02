-- liquibase formatted sql

-- changeset codex:rank_cards_period_allow_flags
ALTER TABLE mc_backend.rank_cards
    ADD COLUMN IF NOT EXISTS allow_month BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_three_months BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_year BOOLEAN DEFAULT TRUE;

UPDATE mc_backend.rank_cards SET allow_month = TRUE WHERE allow_month IS NULL;
UPDATE mc_backend.rank_cards SET allow_three_months = TRUE WHERE allow_three_months IS NULL;
UPDATE mc_backend.rank_cards SET allow_year = TRUE WHERE allow_year IS NULL;

ALTER TABLE mc_backend.rank_cards
    ALTER COLUMN allow_month SET NOT NULL,
    ALTER COLUMN allow_three_months SET NOT NULL,
    ALTER COLUMN allow_year SET NOT NULL;
