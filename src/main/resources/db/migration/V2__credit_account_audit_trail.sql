-- Hibernate Envers audit trail for credit_accounts: every balance/status change
-- (credit granted, order-driven increase, nightly overdue-sweep escalation) is
-- dispute-sensitive, so a full revision history is kept rather than only the
-- latest snapshot. DDL captured from Hibernate's own schema generation for the
-- @Audited CreditAccount entity, then hand-committed here since the app owns
-- its schema via Flyway (ddl-auto=validate) rather than auto-generation.

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS revinfo (
    rev      INTEGER NOT NULL PRIMARY KEY,
    revtstmp BIGINT
);

CREATE TABLE IF NOT EXISTS credit_accounts_aud (
    id                 UUID NOT NULL,
    rev                INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype            SMALLINT,
    credit_limit       NUMERIC(38,2),
    current_balance    NUMERIC(38,2),
    payment_term_days  INTEGER,
    status             VARCHAR(20),
    updated_at         TIMESTAMPTZ,
    creditor_org_id    UUID,
    debtor_org_id      UUID,
    PRIMARY KEY (rev, id),
    CONSTRAINT credit_accounts_aud_status_check
        CHECK (status IN ('GOOD_STANDING', 'OVERDUE', 'LOCKED'))
);
