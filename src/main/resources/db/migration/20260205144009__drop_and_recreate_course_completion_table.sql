-- drop the old table if it exists and recreate it
DROP TABLE IF EXISTS ete_course_events;
CREATE TABLE ete_course_completion_events (
                                   id UUID PRIMARY KEY,
                                   first_name TEXT NOT NULL,
                                   last_name TEXT NOT NULL,
                                   date_of_birth DATE NOT NULL,
                                   region TEXT NOT NULL,
                                   email TEXT NOT NULL,
                                   course_name TEXT NOT NULL,
                                   course_type TEXT NOT NULL,
                                   provider TEXT NOT NULL,
                                   status TEXT NOT NULL,
                                   completion_date DATE NOT NULL,
                                   attempts INT,
                                   total_time_minutes BIGINT NOT NULL,
                                   expected_time_minutes BIGINT NOT NULL,
                                   external_reference TEXT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add index on commonly queried fields
CREATE INDEX idx_ete_course_completion_events_external_id ON ete_course_completion_events(external_reference);
CREATE INDEX idx_ete_course_completion_events_created_at ON ete_course_completion_events(created_at);