CREATE TABLE adjustments (
    id uuid PRIMARY KEY,
    delius_id bigint NOT NULL,
    crn text NOT NULL,
    delius_event_number integer NOT NULL,
    type text NOT NULL,
    minutes integer NOT NULL,
    date date NOT NULL,
    reason_id uuid NOT NULL REFERENCES adjustment_reasons(id),
    appointment_id uuid REFERENCES appointments(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE adjustment_events
    ADD COLUMN adjustment_id uuid REFERENCES adjustments(id);
