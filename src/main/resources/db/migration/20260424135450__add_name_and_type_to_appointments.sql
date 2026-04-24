ALTER TABLE appointments ADD COLUMN first_name TEXT NULL;
ALTER TABLE appointments ADD COLUMN last_name TEXT NULL;

ALTER TABLE appointments ADD COLUMN project_type_id UUID NULL;
ALTER TABLE appointments ADD FOREIGN KEY (project_type_id) REFERENCES project_types(id);
