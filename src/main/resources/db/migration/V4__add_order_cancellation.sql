ALTER TABLE orders
    ADD COLUMN cancel_reason VARCHAR(255),
    ADD COLUMN cancelled_by  VARCHAR(150),
    ADD COLUMN cancelled_at  TIMESTAMP;

CREATE INDEX idx_orders_status ON orders (status);
