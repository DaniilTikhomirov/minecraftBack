-- liquibase formatted sql

-- changeset codex:rank_cards_description_text
-- Преобразуем массив TEXT[] в простой TEXT с объединением по переводу строки.
ALTER TABLE mc_backend.rank_cards
    ALTER COLUMN description TYPE TEXT
        USING array_to_string(description, E'\n');

