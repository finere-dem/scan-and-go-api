-- Boutiques (retail points) are distinct from warehouses (storage/logistics):
-- an organization can open several customer-facing shops, each staffed
-- independently, while its warehouses stay purely about stock holding.
CREATE TABLE boutiques (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID NOT NULL REFERENCES organizations(id),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    address     TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_boutique_org_code UNIQUE (org_id, code)
);
CREATE INDEX idx_boutiques_org_id ON boutiques(org_id);

-- Staff scoped to a single warehouse or boutique rather than the whole
-- organization - a LOGISTICS_OPERATOR manages one depot, a BOUTIQUE_STAFF
-- one shop, without seeing or touching the rest of the org's data.
ALTER TABLE users ADD COLUMN assigned_warehouse_id UUID REFERENCES warehouses(id);
ALTER TABLE users ADD COLUMN assigned_boutique_id UUID REFERENCES boutiques(id);

ALTER TABLE users DROP CONSTRAINT chk_user_role;
ALTER TABLE users ADD CONSTRAINT chk_user_role CHECK (role IN (
    'ROLE_SUPER_ADMIN', 'ROLE_IMPORTER_ADMIN', 'ROLE_LOGISTICS_OPERATOR',
    'ROLE_WHOLESALER_ADMIN', 'ROLE_RETAILER_ADMIN', 'ROLE_BOUTIQUE_STAFF', 'ROLE_CONSUMER'
));
