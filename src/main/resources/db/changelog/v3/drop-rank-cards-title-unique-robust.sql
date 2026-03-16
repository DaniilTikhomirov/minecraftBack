-- liquibase formatted sql

-- changeset backend:drop-rank-cards-title-unique-v3
-- Повторное снятие UNIQUE по title (если v2 не применился или ограничение осталось)
ALTER TABLE mc_backend.rank_cards DROP CONSTRAINT IF EXISTS rank_cards_title_key;
