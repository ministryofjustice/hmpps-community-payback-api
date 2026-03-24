CREATE TABLE appointments (
     id uuid NOT NULL,
     delius_id bigint NOT NULL,
     crn text NOT NULL,
     delius_event_number bigint NOT NULL,
     created_by_community_payback bool NOT NULL,
     CONSTRAINT appointments_pk PRIMARY KEY (id)
);