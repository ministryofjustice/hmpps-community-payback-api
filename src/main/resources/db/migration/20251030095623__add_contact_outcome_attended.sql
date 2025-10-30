ALTER TABLE contact_outcomes ADD attended bool NOT NULL DEFAULT FALSE;
UPDATE contact_outcomes SET attended = TRUE WHERE code IN ('ATTC','ATCH','AFTC','AFDA','ATSH','ATSS');