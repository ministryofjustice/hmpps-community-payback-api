INSERT INTO contact_outcomes_groups (contact_outcome_entity_id, contact_outcome_group)
SELECT id, 'AVAILABLE_FOR_ETE'
FROM contact_outcomes WHERE code IN (
    'ATTC', -- Attended - Complied
    'AFTC', -- Attended - Failed to Comply
    'UAAB'  -- Unacceptable Absence
);
