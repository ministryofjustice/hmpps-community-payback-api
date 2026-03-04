CREATE TABLE community_campus_pdus (
  id uuid NOT NULL,
  "name" text NOT NULL,
  CONSTRAINT community_campus_pdus_pk PRIMARY KEY (id)
);

INSERT INTO community_campus_pdus (id, name)
VALUES (gen_random_uuid(), 'East Midlands'),
       (gen_random_uuid(), 'East of England'),
       (gen_random_uuid(), 'Greater Manchester'),
       (gen_random_uuid(), 'Kent, Surrey and Sussex'),
       (gen_random_uuid(), 'London'),
       (gen_random_uuid(), 'North East'),
       (gen_random_uuid(), 'North West'),
       (gen_random_uuid(), 'South Central'),
       (gen_random_uuid(), 'South West'),
       (gen_random_uuid(), 'Wales'),
       (gen_random_uuid(), 'West Midlands'),
       (gen_random_uuid(), 'Yorks & Humber');
