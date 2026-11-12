CREATE TABLE product_images (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type VARCHAR(50) NOT NULL,
    data         BYTEA       NOT NULL,
    size_bytes   INT         NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

ALTER TABLE products
    ADD COLUMN image_id UUID REFERENCES product_images (id) ON DELETE SET NULL;

CREATE INDEX idx_products_image ON products (image_id);

-- Old column from V2 that was never used
ALTER TABLE products DROP COLUMN image_url;