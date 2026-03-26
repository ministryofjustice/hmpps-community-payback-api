-- replace completion_date with completion_date_time in the ete course completion events table

ALTER TABLE ete_course_completion_events
ADD COLUMN completion_date_time TIMESTAMPTZ;

UPDATE ete_course_completion_events
SET completion_date_time = completion_date::TIMESTAMPTZ;

ALTER TABLE ete_course_completion_events
ALTER COLUMN completion_date_time SET NOT NULL;

ALTER TABLE ete_course_completion_events
DROP COLUMN completion_date;