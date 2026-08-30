-- liquibase formatted sql

-- changeset codex:sundry_catalog
CREATE TABLE mc_backend.sundry (
    id                   BIGSERIAL PRIMARY KEY,
    title                VARCHAR(255),
    subtitle             VARCHAR(255),
    description          VARCHAR(1000),
    detailed_description TEXT,
    image_url            VARCHAR(5000),
    price                INTEGER              NOT NULL,
    active               BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX idx_sundry_active ON mc_backend.sundry (active, id);
