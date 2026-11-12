CREATE TABLE held_carts (
    id            BIGSERIAL    PRIMARY KEY,
    cashier_email VARCHAR(150) NOT NULL,
    label         VARCHAR(60),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_held_carts_cashier ON held_carts (cashier_email);

CREATE TABLE held_cart_items (
    held_cart_id BIGINT NOT NULL REFERENCES held_carts (id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL REFERENCES products (id),
    quantity     INT    NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (held_cart_id, product_id)
);