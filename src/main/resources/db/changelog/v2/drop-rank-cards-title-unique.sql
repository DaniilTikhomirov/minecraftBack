-- liquibase formatted sql

-- changeset backend:drop-rank-cards-title-unique
ALTER TABLE mc_backend.rank_cards DROP CONSTRAINT IF EXISTS rank_cards_title_key;
