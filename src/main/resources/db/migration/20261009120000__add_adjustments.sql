CREATE TABLE adjustments (
    id uuid PRIMARY KEY,
    delius_adjustment_id bigint NOT NULL,
    crn text NOT NULL,
    delius_event_number int NOT NULL,
    adjustment_type text NOT NULL,
    adjustment_date date NOT NULL,
    reason_code text NOT NULL,
    reason_name text NOT NULL,
    minutes int NOT NULL,
    adjustment_reason_id uuid REFERENCES adjustment_reasons(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX adjustments_crn_event_idx ON adjustments (crn, delius_event_number);
