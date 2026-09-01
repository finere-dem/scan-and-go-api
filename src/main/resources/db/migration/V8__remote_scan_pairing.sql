-- A short-lived pairing session between a desktop screen (which creates it and
-- polls for the result) and a phone's browser camera (which reads the code and
-- posts it back) - the phone never authenticates, it only knows this one-time,
-- unguessable session id, so the submit endpoint has to be public.
CREATE TABLE scan_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by_user_id  UUID NOT NULL REFERENCES users(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scanned_code        VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_scan_session_status CHECK (status IN ('PENDING', 'CONSUMED', 'EXPIRED'))
);
