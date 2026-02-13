CREATE TABLE contact_outcomes_groups (
    contact_outcome_entity_id UUID,
    contact_outcome_group TEXT NOT NULL
);

ALTER TABLE contact_outcomes_groups ADD CONSTRAINT contact_outcomes_groups_contact_outcomes_fk FOREIGN KEY (contact_outcome_entity_id) REFERENCES contact_outcomes(id);
CREATE INDEX contact_outcomes_groups_contact_outcome_entity_id_idx ON contact_outcomes_groups (contact_outcome_entity_id);
ALTER TABLE contact_outcomes_groups ADD CONSTRAINT contact_outcomes_groups_unique UNIQUE (contact_outcome_entity_id,contact_outcome_group);

INSERT INTO contact_outcomes_groups (contact_outcome_entity_id, contact_outcome_group)
SELECT id, 'AVAILABLE_TO_ADMIN'
FROM contact_outcomes WHERE code IN (
    'RSOF',
    'RSSR',
    'ATSH',
    'UAAB',
    'ATTC',
    'ATSS',
    'ATCH',
    'AFTC',
    'ATFI',
    'AASD'
);

