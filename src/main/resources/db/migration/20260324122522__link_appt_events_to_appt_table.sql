-- we only currently have test data in this table
delete from appointment_events;

ALTER TABLE appointment_events ADD appointment_id uuid NOT NULL;
ALTER TABLE appointment_events ADD CONSTRAINT appointment_events_appointments_fk FOREIGN KEY (appointment_id) REFERENCES appointments(id);
