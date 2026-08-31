-- Purchase requests: an order a PURCHASE_STAFF account initiates does not
-- become a real order right away. It sits here as PENDING until the org's
-- owner (WHOLESALER_ADMIN/RETAILER_ADMIN) approves it - only then does it
-- turn into a real orders row (stock allocated, invoice created, same as
-- today's direct-order path). Rejected requests never touch stock at all.
CREATE TABLE purchase_requests (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_org_id             UUID NOT NULL REFERENCES organizations(id),
    seller_org_id            UUID NOT NULL REFERENCES organizations(id),
    requested_by_user_id     UUID NOT NULL REFERENCES users(id),
    payment_mode             VARCHAR(20) NOT NULL,
    receiving_warehouse_id   UUID REFERENCES warehouses(id),
    receiving_boutique_id    UUID REFERENCES boutiques(id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason         TEXT,
    decided_by_user_id       UUID REFERENCES users(id),
    decided_at               TIMESTAMPTZ,
    resulting_order_id       UUID REFERENCES orders(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_purchase_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_purchase_request_receiving_single_location CHECK (
        NOT (receiving_warehouse_id IS NOT NULL AND receiving_boutique_id IS NOT NULL)
    )
);
CREATE INDEX idx_purchase_requests_buyer_org_id ON purchase_requests(buyer_org_id);
CREATE INDEX idx_purchase_requests_status ON purchase_requests(status);

CREATE TABLE purchase_request_items (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_request_id   UUID NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    product_id            UUID NOT NULL REFERENCES products(id),
    warehouse_id          UUID NOT NULL REFERENCES warehouses(id),
    quantity              INTEGER NOT NULL,
    CONSTRAINT chk_purchase_request_item_qty CHECK (quantity > 0)
);
CREATE INDEX idx_purchase_request_items_request_id ON purchase_request_items(purchase_request_id);
