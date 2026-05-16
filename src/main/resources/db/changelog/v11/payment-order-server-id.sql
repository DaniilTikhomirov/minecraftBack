-- liquibase formatted sql

-- changeset codex:payment_order_server_id
ALTER TABLE mc_backend.payment_order
    ADD COLUMN IF NOT EXISTS server_id VARCHAR(64) NOT NULL DEFAULT 'anarchy-1';
