CREATE TABLE adjustment_reasons (
    id uuid NOT NULL,
    delius_code text NOT NULL,
    name text NOT NULL,
    max_minutes_allowed int NOT NULL,
    CONSTRAINT adjustment_reasons_pk PRIMARY KEY (id),
    CONSTRAINT adjustment_reasons_max_mins_greater_than_0 CHECK (max_minutes_allowed > 0)
);

INSERT INTO adjustment_reasons (id, delius_code, name, max_minutes_allowed)
VALUES (gen_random_uuid(), 'TTX', 'Travel Time', 180);