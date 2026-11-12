CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150)   NOT NULL,
    sku                 VARCHAR(50)    NOT NULL UNIQUE,
    barcode             VARCHAR(50)    UNIQUE,
    price               NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    cost_price          NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    stock_quantity      INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    low_stock_threshold INT            NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    image_url           VARCHAR(255),
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id         BIGINT REFERENCES categories (id),
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_name ON products (LOWER(name));
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE stock_movements (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products (id),
    type        VARCHAR(20)  NOT NULL,
    quantity    INT          NOT NULL,
    stock_after INT          NOT NULL,
    reason      VARCHAR(255),
    created_by  VARCHAR(150) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product ON stock_movements (product_id, created_at DESC);