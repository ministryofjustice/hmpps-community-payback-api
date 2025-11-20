CREATE TABLE ete_course_events (
  id uuid NOT NULL,
  crn text NOT NULL,
  course_name text NOT NULL,
  total_time_minutes bigint NOT NULL,
  attempts int NOT NULL,
  status text NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT ete_course_events_pk PRIMARY KEY (id)
);
