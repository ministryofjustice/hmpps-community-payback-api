-- we only currently have test data in this table
delete from appointment_events;
delete from appointments;

ALTER TABLE appointments ADD provider_code text NOT NULL;
