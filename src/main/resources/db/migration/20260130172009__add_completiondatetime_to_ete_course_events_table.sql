-- add completion date time to the ete course events table

ALTER TABLE ete_course_events
ADD COLUMN completion_date_time TIMESTAMPTZ;