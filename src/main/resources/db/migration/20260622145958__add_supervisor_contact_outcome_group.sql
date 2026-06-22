INSERT INTO contact_outcomes_groups (contact_outcome_entity_id, contact_outcome_group)
SELECT id, 'AVAILABLE_TO_SUPERVISOR'
FROM contact_outcomes
WHERE name IN (
    'Attended - Complied',
    'Attended - Failed to Comply',
    'Attended - Sent Home (behaviour)',
    'Attended - Sent Home (service issues)',
    'Unacceptable Absence'
);
