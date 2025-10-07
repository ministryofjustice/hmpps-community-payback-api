CREATE TABLE appointment_drafts
(
    id                     UUID PRIMARY KEY,
    appointment_delius_id  BIGINT UNIQUE NOT NULL,
    data                   JSONB         NOT NULL,
    delius_last_updated_at TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
