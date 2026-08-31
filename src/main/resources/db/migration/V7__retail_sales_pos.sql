-- The POS/checkout sale: a boutique selling straight to a walk-in consumer,
-- as opposed to the B2B orders/purchase_requests tables (organization buying
-- from organization). Distinct concept, distinct table.
CREATE TABLE retail_sales (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    boutique_id       UUID NOT NULL REFERENCES boutiques(id),
    sold_by_user_id   UUID NOT NULL REFERENCES users(id),
    total_amount      NUMERIC(38,2) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_retail_sales_boutique_id ON retail_sales(boutique_id);

CREATE TABLE retail_sale_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retail_sale_id    UUID NOT NULL REFERENCES retail_sales(id) ON DELETE CASCADE,
    product_id        UUID NOT NULL REFERENCES products(id),
    lot_id            UUID NOT NULL REFERENCES product_lots(id),
    quantity          INTEGER NOT NULL,
    unit_price        NUMERIC(38,2) NOT NULL,
    subtotal          NUMERIC(38,2) NOT NULL,
    CONSTRAINT chk_retail_sale_item_qty CHECK (quantity > 0)
);
CREATE INDEX idx_retail_sale_items_sale_id ON retail_sale_items(retail_sale_id);
