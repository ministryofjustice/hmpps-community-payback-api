CREATE TABLE session_supervisors (
    project_code text NOT NULL,
    day date NOT NULL,
    supervisor_code varchar NOT NULL,
    allocated_by_username varchar NOT NULL,
    created_at varchar NOT NULL,
    updated_at varchar NOT NULL,
    CONSTRAINT session_supervisors_pk PRIMARY KEY (project_code,day)
);
CREATE INDEX session_supervisors_supervisor_code_idx ON session_supervisors (supervisor_code);
