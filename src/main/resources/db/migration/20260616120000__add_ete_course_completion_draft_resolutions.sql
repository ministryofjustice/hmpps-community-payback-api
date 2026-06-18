CREATE TABLE ete_course_completion_draft_resolutions
(
    id                             UUID        NOT NULL,
    ete_course_completion_event_id UUID        NOT NULL,
    crn                            VARCHAR(255),
    team_code                      VARCHAR(255),
    project_code                   VARCHAR(255),
    appointment_id_to_update       BIGINT,
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ete_course_completion_draft_resolutions_pk PRIMARY KEY (id),
    CONSTRAINT ete_course_completion_draft_resolutions_event_fk
        FOREIGN KEY (ete_course_completion_event_id)
            REFERENCES ete_course_completion_events (id),
    CONSTRAINT ete_course_completion_draft_resolutions_event_unique
        UNIQUE (ete_course_completion_event_id)
);
