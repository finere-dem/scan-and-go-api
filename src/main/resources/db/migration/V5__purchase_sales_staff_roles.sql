-- Strict buy/sell duty separation: an org can now delegate purchasing to a
-- dedicated employee (ROLE_PURCHASE_STAFF) and sales/pricing to a different
-- one (ROLE_SALES_STAFF), rather than only the org admin - who could
-- previously do both - being able to place orders or set prices.
ALTER TABLE users DROP CONSTRAINT chk_user_role;
ALTER TABLE users ADD CONSTRAINT chk_user_role CHECK (role IN (
    'ROLE_SUPER_ADMIN', 'ROLE_IMPORTER_ADMIN', 'ROLE_LOGISTICS_OPERATOR',
    'ROLE_WHOLESALER_ADMIN', 'ROLE_RETAILER_ADMIN', 'ROLE_BOUTIQUE_STAFF',
    'ROLE_PURCHASE_STAFF', 'ROLE_SALES_STAFF', 'ROLE_CONSUMER'
));
