-- we only currently have test data in this table
delete from appointment_outcomes;

ALTER TABLE appointment_outcomes ADD delius_event_number int8 NOT NULL;
ALTER TABLE appointment_outcomes ADD crn varchar NOT NULL;
