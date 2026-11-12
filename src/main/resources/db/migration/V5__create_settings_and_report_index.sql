CREATE TABLE IF NOT EXISTS settings (
    id                BIGINT         PRIMARY KEY CHECK (id = 1),
    store_name        VARCHAR(100)   NOT NULL,
    address           VARCHAR(255),
    phone             VARCHAR(30),
    tax_rate          NUMERIC(5, 2)  NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 100),
    usd_to_khr        NUMERIC(10, 2) NOT NULL CHECK (usd_to_khr > 0),
    default_low_stock INT            NOT NULL CHECK (default_low_stock >= 0),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(150)
);

-- Speeds up report generation (filtering by status + date)
CREATE INDEX IF NOT EXISTS   idx_orders_status_created ON orders (status, created_at);