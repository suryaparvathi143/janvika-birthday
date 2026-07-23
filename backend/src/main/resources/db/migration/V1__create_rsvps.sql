CREATE TABLE rsvps (
    id BIGSERIAL PRIMARY KEY,
    guest_name VARCHAR(100) NOT NULL,
    email VARCHAR(160) NOT NULL,
    attending BOOLEAN NOT NULL,
    party_size INTEGER NOT NULL CHECK (party_size BETWEEN 0 AND 20),
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rsvps_created_at ON rsvps (created_at DESC);

