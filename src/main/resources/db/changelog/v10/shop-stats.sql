-- liquibase formatted sql

-- changeset minecraftback:shop_stats_v10
CREATE TABLE mc_backend.shop_stats_meta (
    id                         SMALLINT PRIMARY KEY,
    period_started_at          TIMESTAMPTZ,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    visit_count                BIGINT      NOT NULL DEFAULT 0,
    completed_purchase_orders  BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE mc_backend.shop_stats_product (
    product_key      VARCHAR(384) PRIMARY KEY,
    display_label    VARCHAR(500) NOT NULL,
    purchase_count   BIGINT       NOT NULL DEFAULT 0,
    revenue_kopecks  BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE mc_backend.shop_stats_category (
    category         VARCHAR(32) PRIMARY KEY,
    purchase_count   BIGINT      NOT NULL DEFAULT 0,
    revenue_kopecks  BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE mc_backend.shop_stats_monthly (
    product_key      VARCHAR(384) NOT NULL,
    month_key        VARCHAR(7)  NOT NULL,
    month_label      VARCHAR(64) NOT NULL,
    purchase_count   BIGINT      NOT NULL DEFAULT 0,
    revenue_kopecks  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (product_key, month_key)
);

CREATE INDEX idx_shop_stats_monthly_product ON mc_backend.shop_stats_monthly (product_key);

CREATE TABLE mc_backend.shop_stats_snapshot (
    id           UUID         PRIMARY KEY,
    label        VARCHAR(128) NOT NULL,
    archived_at  TIMESTAMPTZ  NOT NULL,
    data_json    TEXT         NOT NULL
);

CREATE INDEX idx_shop_stats_snapshot_archived ON mc_backend.shop_stats_snapshot (archived_at DESC);

INSERT INTO mc_backend.shop_stats_meta (id, period_started_at, updated_at, visit_count, completed_purchase_orders)
VALUES (1, NULL, NOW(), 0, 0);
