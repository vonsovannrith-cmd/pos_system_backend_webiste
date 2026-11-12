CREATE SEQUENCE order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(30)    NOT NULL UNIQUE,
    cashier_email   VARCHAR(150)   NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(5, 2)  NOT NULL,
    tax_amount      NUMERIC(12, 2) NOT NULL,
    total           NUMERIC(12, 2) NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL,
    amount_paid     NUMERIC(12, 2) NOT NULL,
    change_amount   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
CREATE INDEX idx_orders_cashier ON orders (cashier_email);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL REFERENCES products (id),
    product_name VARCHAR(150)   NOT NULL,
    sku          VARCHAR(50)    NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);