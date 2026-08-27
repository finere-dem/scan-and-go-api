-- Scan & Go B2B platform - initial schema
-- PostgreSQL 16

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- organizations
-- ============================================================
CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    tax_id          VARCHAR(50) NOT NULL,
    rccm            VARCHAR(50),
    org_type        VARCHAR(30) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_KYC',
    phone           VARCHAR(30),
    email           VARCHAR(255),
    address         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_organizations_tax_id UNIQUE (tax_id),
    CONSTRAINT chk_org_type CHECK (org_type IN ('SUPER_ADMIN', 'IMPORTER', 'WHOLESALER', 'RETAILER')),
    CONSTRAINT chk_org_status CHECK (status IN ('PENDING_KYC', 'ACTIVE', 'SUSPENDED'))
);

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID REFERENCES organizations(id),
    phone           VARCHAR(30) NOT NULL,
    email           VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    role            VARCHAR(40) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_user_role CHECK (role IN (
        'ROLE_SUPER_ADMIN', 'ROLE_IMPORTER_ADMIN', 'ROLE_LOGISTICS_OPERATOR',
        'ROLE_WHOLESALER_ADMIN', 'ROLE_RETAILER_ADMIN', 'ROLE_CONSUMER'
    ))
);
CREATE INDEX idx_users_org_id ON users(org_id);

-- ============================================================
-- warehouses
-- ============================================================
CREATE TABLE warehouses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID NOT NULL REFERENCES organizations(id),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    address     TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_warehouse_org_code UNIQUE (org_id, code)
);
CREATE INDEX idx_warehouses_org_id ON warehouses(org_id);

-- ============================================================
-- products
-- ============================================================
CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    importer_id     UUID NOT NULL REFERENCES organizations(id),
    sku             VARCHAR(100) NOT NULL,
    ean13           VARCHAR(13),
    name            VARCHAR(255) NOT NULL,
    brand           VARCHAR(255),
    category        VARCHAR(100),
    packaging_type  VARCHAR(100),
    units_per_box   INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_importer_sku UNIQUE (importer_id, sku)
);
CREATE INDEX idx_products_ean13 ON products(ean13);
CREATE INDEX idx_products_importer_id ON products(importer_id);

-- ============================================================
-- product_lots
-- ============================================================
CREATE TABLE product_lots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id),
    warehouse_id        UUID NOT NULL REFERENCES warehouses(id),
    lot_number          VARCHAR(100) NOT NULL,
    mfg_date            DATE NOT NULL,
    exp_date            DATE NOT NULL,
    initial_quantity    INTEGER NOT NULL,
    current_quantity    INTEGER NOT NULL,
    unit_cost           NUMERIC(38,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_lot_qty CHECK (current_quantity >= 0),
    CONSTRAINT chk_lot_dates CHECK (exp_date > mfg_date),
    CONSTRAINT chk_lot_status CHECK (status IN ('ACTIVE', 'DEPLETED', 'EXPIRED'))
);
CREATE INDEX idx_product_lots_product_id ON product_lots(product_id);
CREATE INDEX idx_product_lots_warehouse_id ON product_lots(warehouse_id);
CREATE INDEX idx_product_lots_exp_date ON product_lots(exp_date);

-- ============================================================
-- pricing_policies
-- ============================================================
CREATE TABLE pricing_policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_org_id   UUID NOT NULL REFERENCES organizations(id),
    product_id      UUID NOT NULL REFERENCES products(id),
    target_org_type VARCHAR(30) NOT NULL,
    unit_price      NUMERIC(38,2) NOT NULL,
    min_order_qty   INTEGER NOT NULL DEFAULT 1,
    currency        VARCHAR(10) NOT NULL DEFAULT 'XOF',
    CONSTRAINT uq_pricing_tier UNIQUE (seller_org_id, product_id, target_org_type),
    CONSTRAINT chk_pricing_target CHECK (target_org_type IN ('WHOLESALER', 'RETAILER'))
);

-- ============================================================
-- local_retail_prices
-- ============================================================
CREATE TABLE local_retail_prices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retailer_org_id     UUID NOT NULL REFERENCES organizations(id),
    product_id          UUID NOT NULL REFERENCES products(id),
    consumer_price      NUMERIC(38,2) NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'XOF',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_retail_price UNIQUE (retailer_org_id, product_id)
);

-- ============================================================
-- credit_accounts
-- ============================================================
CREATE TABLE credit_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creditor_org_id     UUID NOT NULL REFERENCES organizations(id),
    debtor_org_id       UUID NOT NULL REFERENCES organizations(id),
    credit_limit        NUMERIC(38,2) NOT NULL DEFAULT 0,
    current_balance     NUMERIC(38,2) NOT NULL DEFAULT 0,
    payment_term_days   INTEGER NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'GOOD_STANDING',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_credit_relation UNIQUE (creditor_org_id, debtor_org_id),
    CONSTRAINT chk_credit_term CHECK (payment_term_days IN (0, 30, 60, 90)),
    CONSTRAINT chk_credit_status CHECK (status IN ('GOOD_STANDING', 'OVERDUE', 'LOCKED'))
);

-- ============================================================
-- orders
-- ============================================================
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50) NOT NULL,
    buyer_org_id    UUID NOT NULL REFERENCES organizations(id),
    seller_org_id   UUID NOT NULL REFERENCES organizations(id),
    total_amount    NUMERIC(38,2) NOT NULL DEFAULT 0,
    payment_mode    VARCHAR(20) NOT NULL,
    order_status    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    client_sync_id  UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT uq_orders_client_sync_id UNIQUE (client_sync_id),
    CONSTRAINT chk_payment_mode CHECK (payment_mode IN ('CASH', 'ON_DELIVERY', 'CREDIT_30', 'CREDIT_60', 'CREDIT_90')),
    CONSTRAINT chk_order_status CHECK (order_status IN ('DRAFT', 'SUBMITTED', 'CONFIRMED', 'DELIVERED', 'CANCELLED'))
);
CREATE INDEX idx_orders_buyer_org_id ON orders(buyer_org_id);
CREATE INDEX idx_orders_seller_org_id ON orders(seller_org_id);

-- ============================================================
-- order_items
-- ============================================================
CREATE TABLE order_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders(id),
    product_id  UUID NOT NULL REFERENCES products(id),
    lot_id      UUID REFERENCES product_lots(id),
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC(38,2) NOT NULL,
    subtotal    NUMERIC(38,2) NOT NULL,
    CONSTRAINT chk_item_qty CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- ============================================================
-- invoices
-- ============================================================
CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    invoice_number  VARCHAR(50) NOT NULL,
    issue_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE NOT NULL,
    amount_due      NUMERIC(38,2) NOT NULL,
    amount_paid     NUMERIC(38,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invoices_order_id UNIQUE (order_id),
    CONSTRAINT uq_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT chk_invoice_status CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'DEFAULT'))
);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);

-- ============================================================
-- qr_matrix_tokens
-- ============================================================
CREATE TABLE qr_matrix_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products(id),
    lot_id          UUID REFERENCES product_lots(id),
    public_token    VARCHAR(100) NOT NULL,
    signature_hash  VARCHAR(255) NOT NULL,
    matrix_type     VARCHAR(20) NOT NULL,
    CONSTRAINT uq_qr_public_token UNIQUE (public_token),
    CONSTRAINT chk_qr_matrix_type CHECK (matrix_type IN ('PRODUCT_GLOBAL', 'LOT_SPECIFIC', 'SHELF_POSTER'))
);
CREATE INDEX idx_qr_matrix_product_id ON qr_matrix_tokens(product_id);
