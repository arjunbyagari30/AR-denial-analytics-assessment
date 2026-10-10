CREATE TABLE claims (
    id BIGINT NOT NULL,
    payer VARCHAR(100) NOT NULL,
    provider VARCHAR(150) NOT NULL,
    cpt_code VARCHAR(10) NOT NULL,
    billed_amount DECIMAL(12, 2) NOT NULL,
    denial_reason VARCHAR(255) NULL,
    denied_at DATE NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT pk_claims PRIMARY KEY (id),
    CONSTRAINT chk_claims_status CHECK (status IN ('denied', 'paid')),
    CONSTRAINT chk_claims_nonnegative_amount CHECK (billed_amount >= 0),
    CONSTRAINT chk_claims_denial_details CHECK (
        (status = 'denied' AND denial_reason IS NOT NULL AND denied_at IS NOT NULL)
        OR (status = 'paid' AND denial_reason IS NULL)
    )
);

-- Supports denied-claim status filtering followed by denied-date range queries.
CREATE INDEX idx_claims_status_denied_at ON claims (status, denied_at);
