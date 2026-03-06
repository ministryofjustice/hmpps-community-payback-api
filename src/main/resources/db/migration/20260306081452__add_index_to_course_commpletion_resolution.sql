-- Add index to email column on ete_course_completion_events

CREATE INDEX idx_ete_course_completion_events_email
    ON ete_course_completion_events (email);


CREATE INDEX idx_ete_course_completion_events_office_course
    ON ete_course_completion_events (office, course_name);
