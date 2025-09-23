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

-- Drop old column if it exists
ALTER TABLE appointment_outcomes
    DROP COLUMN IF EXISTS project_type_delius_id;

-- Add new nullable column
ALTER TABLE appointment_outcomes
    ADD COLUMN project_type_id UUID NULL;

-- Add foreign key constraint (nullable FK is allowed)
ALTER TABLE appointment_outcomes
    ADD CONSTRAINT fk_appointments_project_type
        FOREIGN KEY (project_type_id)
            REFERENCES project_types (id);
