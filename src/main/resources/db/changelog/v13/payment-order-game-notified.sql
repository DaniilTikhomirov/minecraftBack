-- liquibase formatted sql

-- changeset codex:payment_order_game_notified_at
ALTER TABLE mc_backend.payment_order
    ADD COLUMN IF NOT EXISTS game_notified_at TIMESTAMPTZ;

-- Уже оплаченные заказы считаем доставленными, чтобы не перевыдать донат при деплое.
UPDATE mc_backend.payment_order
SET game_notified_at = COALESCE(updated_at, created_at, NOW())
WHERE status = 'PAID'
  AND game_notified_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_payment_order_paid_unnotified
    ON mc_backend.payment_order (created_at)
    WHERE status = 'PAID' AND game_notified_at IS NULL;
