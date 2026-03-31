-- liquibase formatted sql

-- changeset codex:rank_cards_drop_legacy_price
-- В старой схеме могла остаться колонка price с NOT NULL, которая больше не используется.
ALTER TABLE mc_backend.rank_cards
    DROP COLUMN IF EXISTS price;

