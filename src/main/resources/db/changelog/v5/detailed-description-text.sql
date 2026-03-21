-- liquibase formatted sql

-- changeset codex:add_detailed_description_privileges_cases_main_news
ALTER TABLE mc_backend.rank_cards
    ADD COLUMN IF NOT EXISTS detailed_description TEXT;

ALTER TABLE mc_backend.cases
    ADD COLUMN IF NOT EXISTS detailed_description TEXT;

ALTER TABLE mc_backend.main_news
    ADD COLUMN IF NOT EXISTS detailed_description TEXT;
