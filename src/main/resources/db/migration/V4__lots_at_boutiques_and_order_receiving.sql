-- A lot previously could only live in a warehouse, which meant a buyer's own
-- inventory was never modeled: receiving a delivery only decremented the
-- seller's stock, never created stock for the buyer. A lot can now belong to
-- either a warehouse OR a boutique (a retail point holds its own stock too),
-- with exactly one location required.
ALTER TABLE product_lots ALTER COLUMN warehouse_id DROP NOT NULL;
ALTER TABLE product_lots ADD COLUMN boutique_id UUID REFERENCES boutiques(id);
ALTER TABLE product_lots ADD CONSTRAINT chk_lot_single_location CHECK (
    (warehouse_id IS NOT NULL AND boutique_id IS NULL)
    OR (warehouse_id IS NULL AND boutique_id IS NOT NULL)
);
CREATE INDEX idx_product_lots_boutique_id ON product_lots(boutique_id);

-- Where the buyer wants a confirmed order's goods received into their own
-- stock (their warehouse or their boutique) - optional so existing callers
-- that don't care about receiving stock (e.g. a wholesaler just tracking
-- purchases on paper) keep working unchanged.
ALTER TABLE orders ADD COLUMN receiving_warehouse_id UUID REFERENCES warehouses(id);
ALTER TABLE orders ADD COLUMN receiving_boutique_id UUID REFERENCES boutiques(id);
ALTER TABLE orders ADD CONSTRAINT chk_order_receiving_single_location CHECK (
    NOT (receiving_warehouse_id IS NOT NULL AND receiving_boutique_id IS NOT NULL)
);
