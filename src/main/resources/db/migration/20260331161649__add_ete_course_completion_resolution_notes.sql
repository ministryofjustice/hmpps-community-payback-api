UPDATE ete_course_completion_event_resolutions SET resolution = 'DONT_CREDIT_TIME' WHERE resolution = 'COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD';

ALTER TABLE ete_course_completion_event_resolutions ADD notes text NULL;
