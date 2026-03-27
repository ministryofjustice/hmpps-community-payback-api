CREATE TABLE appointment_tasks (
  id uuid NOT NULL,
  appointment_id uuid NOT NULL,
  task_type text NOT NULL,
  created_at timestamptz NOT NULL,
  task_status text NOT NULL,
  decision_made_by_username text,
  decision_made_at timestamptz,
  CONSTRAINT appointment_tasks_pk PRIMARY KEY (id),
  CONSTRAINT appointment_tasks_fk FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE INDEX appointment_tasks_appointment_id_idx ON appointment_tasks(appointment_id);

