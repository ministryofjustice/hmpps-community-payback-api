-- we only currently have test data in this table
DELETE FROM appointment_events;

ALTER TABLE appointment_events ADD triggered_at timestamptz NOT NULL;