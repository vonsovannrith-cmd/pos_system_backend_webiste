ALTER TABLE orders
    ADD COLUMN payment_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN exchange_rate    NUMERIC(10, 2),
    ADD COLUMN total_khr        NUMERIC(14, 0);