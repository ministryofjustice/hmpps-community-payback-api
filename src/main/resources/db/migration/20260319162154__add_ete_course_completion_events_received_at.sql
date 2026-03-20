-- this is safe because the table isn't currently in active use
DELETE FROM ete_course_completion_events;

ALTER TABLE ete_course_completion_events ADD received_at timestamptz NOT NULL;
