DELETE FROM contact_outcomes_groups
WHERE contact_outcome_entity_id IN (
    SELECT id
    FROM contact_outcomes
    WHERE name = 'Failed to Comply with other Instruction'
);
