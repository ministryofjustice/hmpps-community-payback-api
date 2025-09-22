CREATE TABLE contact_outcomes
(
    id         UUID PRIMARY KEY,
    code       TEXT NOT NULL,
    name       TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO contact_outcomes (id, code, name)
VALUES (gen_random_uuid(), 'AFDA', 'Attended - FTC Deemed Acceptable'),
       (gen_random_uuid(), 'ATTC', 'Attended - Complied'),
       (gen_random_uuid(), 'AFTC', 'Attended - Failed to Comply'),
       (gen_random_uuid(), 'ATSH', 'Attended - Sent Home (behaviour)'),
       (gen_random_uuid(), 'ATSS', 'Attended - Sent Home (service issues)'),
       (gen_random_uuid(), 'ATFI', 'Failed to Comply with other Instruction'),
       (gen_random_uuid(), 'UAAB', 'Unacceptable Absence'),
       (gen_random_uuid(), 'AAME', 'Acceptable Absence - Medical'),
       (gen_random_uuid(), 'AASD', 'Acceptable Absence - Stood Down'),
       (gen_random_uuid(), 'AAEM', 'Acceptable Absence - Employment'),
       (gen_random_uuid(), 'AAFC', 'Acceptable Absence - Family/ Childcare'),
       (gen_random_uuid(), 'AACL', 'Acceptable Absence - Court/Legal'),
       (gen_random_uuid(), 'AARC', 'Acceptable Absence - RIC'),
       (gen_random_uuid(), 'RSSR', 'Rescheduled - Service Request'),
       (gen_random_uuid(), 'RSOF', 'Rescheduled - PoP Request'),
       (gen_random_uuid(), 'CO10', 'Acceptable Failure - None in following 12 months'),
       (gen_random_uuid(), 'CO38', 'Warrant Outstanding'),
       (gen_random_uuid(), 'CO39', 'YOT Breach - Not Enforceable'),
       (gen_random_uuid(), 'CO40', 'Suspended'),
       (gen_random_uuid(), 'ATCH', 'Attended - Concurrent Hours'),
       (gen_random_uuid(), 'CO05', 'Acceptable Absence-Professional Judgement Decision');


CREATE TABLE appointment_outcomes
(
    id                           UUID PRIMARY KEY,
    appointment_delius_id        BIGINT NOT NULL,
    project_type_delius_id       BIGINT NOT NULL,
    start_time                   TIME   NOT NULL,
    end_time                     TIME   NOT NULL,
    contact_outcome_id           UUID   NOT NULL REFERENCES contact_outcomes (id),
    supervisor_team_delius_id    BIGINT NOT NULL,
    supervisor_officer_delius_id BIGINT NOT NULL,
    notes                        TEXT,
    hi_vis_worn                  BOOLEAN,
    worked_intensively           BOOLEAN,
    penalty_minutes              BIGINT CHECK (penalty_minutes >= 0),
    work_quality                 TEXT CHECK (work_quality IN
                                             ('EXCELLENT', 'GOOD', 'NOT_APPLICABLE', 'POOR', 'SATISFACTORY',
                                              'UNSATISFACTORY')),
    behaviour                    TEXT CHECK (behaviour IN
                                             ('EXCELLENT', 'GOOD', 'NOT_APPLICABLE', 'POOR', 'SATISFACTORY',
                                              'UNSATISFACTORY')),
    enforcement_action_delius_id BIGINT,
    respond_by                   DATE,
    created_at                   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
