-- liquibase formatted sql

-- changeset codex:rank_cards_subscription_pricing
ALTER TABLE mc_backend.rank_cards
    ADD COLUMN IF NOT EXISTS price_month INT,
    ADD COLUMN IF NOT EXISTS price_three_months INT,
    ADD COLUMN IF NOT EXISTS price_year INT,
    ADD COLUMN IF NOT EXISTS allow_forever BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS price_forever INT;

-- Перенос исторической цены в модель подписки.
UPDATE mc_backend.rank_cards
SET
    price_month = COALESCE(price_month, price),
    price_three_months = COALESCE(price_three_months, price * 3),
    price_year = COALESCE(price_year, price * 12),
    allow_forever = COALESCE(allow_forever, FALSE),
    price_forever = CASE
        WHEN COALESCE(allow_forever, FALSE) THEN COALESCE(price_forever, price)
        ELSE NULL
    END
WHERE price IS NOT NULL;

-- Защита данных.
UPDATE mc_backend.rank_cards SET allow_forever = FALSE WHERE allow_forever IS NULL;

ALTER TABLE mc_backend.rank_cards
    ALTER COLUMN price_month SET NOT NULL,
    ALTER COLUMN price_three_months SET NOT NULL,
    ALTER COLUMN price_year SET NOT NULL,
    ALTER COLUMN allow_forever SET NOT NULL;

-- Старая колонка больше не используется.
ALTER TABLE mc_backend.rank_cards
    DROP COLUMN IF EXISTS price;
