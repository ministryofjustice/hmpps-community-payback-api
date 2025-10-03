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


CREATE TABLE enforcement_actions
(
    id         UUID PRIMARY KEY,
    code       TEXT NOT NULL,
    name       TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO enforcement_actions (ID, CODE, NAME)
VALUES (gen_random_uuid(), 'BRE02', 'Breach Request Actioned'),
       (gen_random_uuid(), 'ROM', 'Refer to Offender Manager'),
       (gen_random_uuid(), 'IBR', 'Breach / Recall Initiated'),
       (gen_random_uuid(), 'NFA', 'No Further Action'),
       (gen_random_uuid(), 'IMB', 'Immediate Breach or Recall'),
       (gen_random_uuid(), 'WLS', 'Enforcement Letter Requested'),
       (gen_random_uuid(), 'EA02', 'First Warning Letter Sent'),
       (gen_random_uuid(), 'EA03', 'Second Warning Letter Sent'),
       (gen_random_uuid(), 'EA05', 'Other Enforcement Letter Sent'),
       (gen_random_uuid(), 'EA06', 'Withdraw Warning Letter'),
       (gen_random_uuid(), 'EA07', 'Withdrawal of Warning'),
       (gen_random_uuid(), 'EA08', 'Breach Letter Sent'),
       (gen_random_uuid(), 'EA09', 'Send Confirmation of Breach'),
       (gen_random_uuid(), 'EA10', 'Breach Confirmation Sent'),
       (gen_random_uuid(), 'EA11', 'Recall Requested'),
       (gen_random_uuid(), 'EA12', 'Decision Pending Response from Person on Probation'),
       (gen_random_uuid(), 'EA13', 'YOT OM Notified'),
       (gen_random_uuid(), 'BRE01', 'Breach Requested'),
       (gen_random_uuid(), 'LCL', 'Licence Compliance Letter Sent');

CREATE TABLE project_types
(
    id         UUID PRIMARY KEY,
    code       TEXT NOT NULL,
    name       TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO project_types (id, code, name)
VALUES (gen_random_uuid(), 'PL', 'CPxASB Rapid Deployment - Project Clean Streets'),
       (gen_random_uuid(), 'ET3', 'ETE - CFO'),
       (gen_random_uuid(), 'ET1', 'ETE - E-learning'),
       (gen_random_uuid(), 'ET5', 'ETE - HMPPS Portal'),
       (gen_random_uuid(), 'UP06', 'ETE- Contracted Provider'),
       (gen_random_uuid(), 'ES', 'Externally Supervised Placement'),
       (gen_random_uuid(), 'NP2', 'Group Placement - National Project'),
       (gen_random_uuid(), 'NP1', 'Group Placement - Regional Project'),
       (gen_random_uuid(), 'WH1', 'Independent Working'),
       (gen_random_uuid(), 'ICP', 'Individual Placement - ICP (Individual Community Placement)'),
       (gen_random_uuid(), 'PIP2', 'Individual Placement - PIP (PoP Identified Placement)'),
       (gen_random_uuid(), 'PS', 'PSAI / PPWS'),
       (gen_random_uuid(), 'PIP', 'PoP Identified Placements'),
       (gen_random_uuid(), 'PI', 'UPW PoP Induction');

CREATE TABLE appointment_outcomes
(
    id                      UUID PRIMARY KEY,
    appointment_delius_id   BIGINT NOT NULL,
    start_time              TIME   NOT NULL,
    end_time                TIME   NOT NULL,
    contact_outcome_id      UUID REFERENCES contact_outcomes (id) NOT NULL,
    supervisor_officer_code TEXT NOT NULL,
    notes                   TEXT,
    hi_vis_worn             BOOLEAN,
    worked_intensively      BOOLEAN,
    penalty_minutes         BIGINT CHECK (penalty_minutes >= 0),
    work_quality            TEXT CHECK (work_quality IN
                                        ('EXCELLENT', 'GOOD', 'NOT_APPLICABLE', 'POOR', 'SATISFACTORY',
                                         'UNSATISFACTORY')),
    behaviour               TEXT CHECK (behaviour IN
                                        ('EXCELLENT', 'GOOD', 'NOT_APPLICABLE', 'POOR', 'SATISFACTORY',
                                         'UNSATISFACTORY')),
    enforcement_action_id   UUID REFERENCES enforcement_actions (id),
    respond_by              DATE,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
