CREATE TABLE whatsapp_guests (
    id BIGSERIAL PRIMARY KEY,
    guest_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(25) NOT NULL UNIQUE,
    invitation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_error VARCHAR(500),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_whatsapp_guests_status ON whatsapp_guests (invitation_status, created_at ASC);
