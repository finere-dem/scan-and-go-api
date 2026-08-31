package com.finere.scan_and_go_api.domain.enums;

public enum UserRole {
    ROLE_SUPER_ADMIN,
    ROLE_IMPORTER_ADMIN,
    ROLE_LOGISTICS_OPERATOR,
    ROLE_WHOLESALER_ADMIN,
    ROLE_RETAILER_ADMIN,
    ROLE_BOUTIQUE_STAFF,
    /** Places purchase orders on the org's behalf; never sees purchase cost of stock it didn't buy
     * itself and is never the same account as ROLE_SALES_STAFF - strict buy/sell duty separation. */
    ROLE_PURCHASE_STAFF,
    /** Handles pricing/sales to downstream customers; purchase cost (ProductLot.unitCost) is
     * hidden from this role even when reading otherwise-visible stock data. */
    ROLE_SALES_STAFF,
    ROLE_CONSUMER
}
