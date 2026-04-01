-- not used
DROP INDEX idx_ete_course_completion_events_external_id;

-- fk
CREATE INDEX adjustment_events_appointment_id_idx ON adjustment_events (appointment_id);
CREATE INDEX appointment_events_appointment_id_idx ON appointment_events (appointment_id);
