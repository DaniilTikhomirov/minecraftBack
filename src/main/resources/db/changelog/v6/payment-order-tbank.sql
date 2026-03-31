-- liquibase formatted sql

-- changeset codex:payment_order_tbank
CREATE TABLE mc_backend.payment_order (
    id                 UUID PRIMARY KEY,
    tbank_order_id     VARCHAR(64)  NOT NULL UNIQUE,
    nickname           VARCHAR(500) NOT NULL,
    email              VARCHAR(255),
    amount_kopecks     BIGINT       NOT NULL,
    product_type       VARCHAR(32)  NOT NULL,
    product_id         BIGINT,
    subscription_period VARCHAR(32),
    quantity           INTEGER,
    tbank_payment_id   VARCHAR(64),
    payment_url        VARCHAR(2000),
    status             VARCHAR(32)  NOT NULL,
    raw_init_response  TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_order_status ON mc_backend.payment_order (status);
CREATE INDEX idx_payment_order_tbank_payment_id ON mc_backend.payment_order (tbank_payment_id);
