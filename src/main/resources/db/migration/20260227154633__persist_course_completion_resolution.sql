CREATE TABLE ete_course_completion_event_resolutions (
    id uuid NOT NULL,
    ete_course_completion_event_id uuid NOT NULL,
    resolution text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by_username text NOT NULL,
    crn text NULL,
    delius_event_number int8 NULL,
    delius_appointment_id int8 NULL,
    delius_appointment_created bool NULL,
    project_code text NULL,
    minutes_credited int8 NULL,
    contact_outcome_id uuid NULL,
    CONSTRAINT ete_course_completion_event_resolutions_pk PRIMARY KEY (id),
    CONSTRAINT ete_course_completion_event_resolutions_contact_outcomes_fk FOREIGN KEY (contact_outcome_id) REFERENCES contact_outcomes(id),
    CONSTRAINT ete_course_completion_event_resolutions_ete_course_completion_events_fk FOREIGN KEY (ete_course_completion_event_id) REFERENCES ete_course_completion_events(id)
);
CREATE INDEX ete_course_completion_event_resolutions_ete_course_completion_event_id_idx ON ete_course_completion_event_resolutions (ete_course_completion_event_id);
