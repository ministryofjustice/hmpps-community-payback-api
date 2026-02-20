-- Add column office to ete_course_completion_events table
ALTER TABLE ete_course_completion_events ADD COLUMN office TEXT NOT NULL DEFAULT 'Office'