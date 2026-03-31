-- liquibase formatted sql

-- changeset codex:wiki_card_table
CREATE TABLE mc_backend.wiki_card (
    id               BIGSERIAL PRIMARY KEY,
    title            TEXT        NOT NULL,
    subtitle         TEXT        NOT NULL DEFAULT '',
    cover_image_url  TEXT        NOT NULL DEFAULT '',
    sort_order       INT         NOT NULL DEFAULT 0,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    article          JSONB       NOT NULL DEFAULT '{"version":1,"blocks":[]}'::jsonb,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wiki_card_active_sort ON mc_backend.wiki_card (active, sort_order, id);
