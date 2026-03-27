CREATE TABLE adjustment_events (
    id uuid NOT NULL,
    event_type text NOT NULL,
    created_at timestamptz NOT NULL,
    triggered_at timestamptz NOT NULL,
    trigger_type text NOT NULL,
    triggered_by text NOT NULL,
    delius_adjustment_id bigint NOT NULL,
    appointment_id uuid NOT NULL,
    adjustment_type text NOT NULL,
    adjustment_minutes int NOT NULL,
    adjustment_date date NOT NULL,
    adjustment_reason_id uuid NOT NULL,
    CONSTRAINT adjustment_events_pk PRIMARY KEY (id),
    CONSTRAINT adjustment_events_adjustment_reasons_fk FOREIGN KEY (adjustment_reason_id) REFERENCES adjustment_reasons(id),
    CONSTRAINT adjustment_events_appointments_fk FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);
