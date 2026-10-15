DROP TABLE session_supervisors;

CREATE TABLE session_supervisors (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    project_code text NOT NULL,
    day date,
    supervisor_code varchar NOT NULL,
    assigned_by_username varchar NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT session_supervisors_pk PRIMARY KEY (id)
);

CREATE INDEX session_supervisors_supervisor_code_idx ON session_supervisors (supervisor_code);
CREATE UNIQUE INDEX session_supervisors_session_assignment_idx
    ON session_supervisors (project_code, day, supervisor_code) WHERE day IS NOT NULL;
CREATE UNIQUE INDEX session_supervisors_project_assignment_idx
    ON session_supervisors (project_code, supervisor_code) WHERE day IS NULL;
