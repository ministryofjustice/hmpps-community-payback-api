INSERT INTO contact_outcomes (id, code, name)
VALUES (gen_random_uuid(), 'CO05', 'Acceptable Absence-Professional Judgement Decision');

ALTER TABLE contact_outcomes ADD COLUMN enforceable BOOLEAN DEFAULT FALSE;

UPDATE contact_outcomes SET enforceable = TRUE WHERE code = 'AFTC';
UPDATE contact_outcomes SET enforceable = TRUE WHERE code = 'ATSH';
UPDATE contact_outcomes SET enforceable = TRUE WHERE code = 'ATFI';
UPDATE contact_outcomes SET enforceable = TRUE WHERE code = 'UAAB';