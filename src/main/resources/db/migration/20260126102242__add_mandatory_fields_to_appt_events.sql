-- we only currently have test data in this table
delete from appointment_events;

ALTER TABLE appointment_events
    ADD event_type text NOT NULL,
    ADD project_code text NOT NULL,
    ADD "date" date NOT NULL;
