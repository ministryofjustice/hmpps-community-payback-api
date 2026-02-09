ALTER TABLE project_types ADD project_type_group text NULL;

UPDATE project_types SET project_type_group = 'GROUP' WHERE code IN('PL', 'NP1', 'NP2');
UPDATE project_types SET project_type_group = 'INDIVIDUAL' WHERE code IN('ES', 'ICP', 'PIP2', 'PSP');
UPDATE project_types SET project_type_group = 'INDUCTION' WHERE code IN('PS', 'PI');