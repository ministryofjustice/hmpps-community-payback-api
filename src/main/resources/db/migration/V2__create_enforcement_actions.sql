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



ALTER TABLE appointment_outcomes
    DROP COLUMN IF EXISTS enforcement_action_delius_id,
    ADD COLUMN enforcement_action_id UUID,
    ADD CONSTRAINT fk_appointments_outcome_enforcement_action
        FOREIGN KEY (enforcement_action_id)
            REFERENCES enforcement_actions (id);