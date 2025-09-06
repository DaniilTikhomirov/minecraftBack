-- liquibase formatted sql

-- changeset danil:create_news_table
CREATE TABLE main_news
(
    id          bigserial PRIMARY KEY,
    title       varchar(255),
    description varchar(1000),
    date        varchar(255),
    active      boolean DEFAULT TRUE NOT NULL,
    image_url   varchar(5000)
);

CREATE TABLE mini_news
(
    id          bigserial PRIMARY KEY,
    title       varchar(255),
    description varchar(5000),
    date        varchar(255),
    active      boolean DEFAULT TRUE NOT NULL,
    image_url   varchar(5000)
);

-- changeset danil:create_admin_table
CREATE TABLE admin_users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) UNIQUE    NOT NULL,
    password   VARCHAR(255)           NOT NULL,
    role       VARCHAR(50)            NOT NULL,
    enabled    BOOLEAN   DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- changeset danil:create_slider_cards
CREATE TABLE rank_cards
(
    id          BIGSERIAL PRIMARY KEY,
    title       varchar(255) UNIQUE  NOT NULL,
    image_url   varchar(5000),
    price       int                  NOT NULL,
    description varchar(5000),
    active      boolean DEFAULT TRUE NOT NULL
);

-- changeset danil:create_cases
CREATE TABLE cases
(
    id          BIGSERIAL PRIMARY KEY,
    title       varchar(255),
    subtitle    varchar(255),
    description varchar(1000),
    image_url   varchar(5000),
    price       integer              NOT NULL,
    active      boolean DEFAULT TRUE NOT NULL
);

-- changeset danil:create_transaction
CREATE TABLE transaction_log
(
    id         varchar(36) PRIMARY KEY,
    status     varchar(255) NOT NULL,
    value      varchar(255),
    currency   varchar(3),
    account_id varchar(500),
    gateway_id varchar(500),
    nick_name  varchar(5000),
    created_at TIMESTAMP,
    test       boolean,
    paid       boolean,
    refundable boolean
);

CREATE TABLE paid_order
(
    id                BIGSERIAL PRIMARY KEY,
    transaction_id    varchar(36) NOT NULL,
    product_name      varchar(255),
    nick_name         varchar(5000),
    processing_status varchar(255),
    CONSTRAINT fk_paid_order_transaction
        FOREIGN KEY (transaction_id)
            REFERENCES transaction_log (id)
);

-- changeset danil:create_value_rate
CREATE TABLE exchange_rate (
    id INTEGER PRIMARY KEY,
    rate DECIMAL(10, 2) NOT NULL
);

