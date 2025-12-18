-- src/main/resources/schema_success.sql
CREATE TABLE IF NOT EXISTS account (
    account_number VARCHAR(64) PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    balance        BIGINT       NOT NULL
);