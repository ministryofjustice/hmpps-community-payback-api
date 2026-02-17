-- Add version column to ete_course_completion_events table
ALTER TABLE ete_course_completion_events ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
