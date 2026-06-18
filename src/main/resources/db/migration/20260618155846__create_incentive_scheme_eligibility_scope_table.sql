CREATE TABLE incentive_scheme_eligibility_scopes (
  id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
  scope TEXT NOT NULL,
  code TEXT NOT NULL,
  description TEXT NOT NULL,
  is_eligible BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT incentive_scheme_eligibility_scopes_unique UNIQUE (scope, code)
);

INSERT INTO incentive_scheme_eligibility_scopes (scope, code, description, is_eligible)
VALUES
    -- Orders in scope
        -- Community orders
    ('OUTCOME', '201', 'CJA - Community Order', TRUE),
    ('OUTCOME', '340', 'SA2020 Community Order', TRUE),
        -- Suspended sentence orders
    ('OUTCOME', '203', 'CJA - Suspended Sentence Order', TRUE),
    ('OUTCOME', '216', 'Suspended Sentence Supn Order', TRUE),
    ('OUTCOME', '341', 'SA2020 Suspended Sentence Order', TRUE),
    ('OUTCOME', '408', 'Suspended Sentence', TRUE),
        -- Community payback orders made in Scotland and transferred to England/Wales
    ('OUTCOME', '234', 'Scottish Community Order (CPA95)', TRUE),
    ('OUTCOME', '235', 'Scottish Comm Payback (CJLA2010)', TRUE),
    -- Orders out of scope
        -- Supervision default orders
    ('OUTCOME', '233', 'ORA Supervision Default Order', FALSE),
        -- Service community orders
        -- (none)
        -- Enforcement orders
    ('OUTCOME', '212', 'Enforcement Order (Pre-CJA)', FALSE),
    ('OUTCOME', '226', 'CJA -Enforcement Order (C&AA 06)', FALSE),
        -- Jersey orders
    ('OUTCOME', '237', 'CS & Probation Order - Jersey', FALSE),
        -- Deferred sentences
    ('OUTCOME', '123', 'Deferred Sentence', FALSE),
    ('OUTCOME', '202', 'CJA - Deferred Sentence', FALSE),
    ('OUTCOME', '407', 'Deferred Sentence  (Pre CJA03)', FALSE),
        -- Youth rehabilitation orders
    ('OUTCOME', '204', 'CJA - Youth Rehabilitation Order', FALSE),
    ('OUTCOME', '342', 'SA2020 Youth Rehab Order', FALSE),
        -- Community service orders imposed in Northern Ireland
        -- (none)

    -- Requirement subtypes
        -- In scope
    ('REQUIREMENT_SUBTYPE', 'W01', 'Regular', TRUE),
    ('REQUIREMENT_SUBTYPE', 'W06', 'Hours Concurrent to Another Order', TRUE),
    ('REQUIREMENT_SUBTYPE', 'W07', 'Hours Consecutive to Another Order', TRUE),
        -- TBC
    ('REQUIREMENT_SUBTYPE', 'W05', 'Z - Hours Outstanding from a Previous Order', FALSE),
    ('REQUIREMENT_SUBTYPE', 'UPCONC', 'UPW Hours to be worked Concurrently', FALSE),
        -- Out of scope
    ('REQUIREMENT_SUBTYPE', 'W03', 'Additional Hours', FALSE),
    ('REQUIREMENT_SUBTYPE', 'W08', 'Hours Outstanding at Sentence Expiry', FALSE);
