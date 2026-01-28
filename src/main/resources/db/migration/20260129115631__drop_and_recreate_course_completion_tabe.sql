-- drop the old table if it exists and recreate it
DROP TABLE IF EXISTS ete_course_events;
CREATE TABLE ete_course_events (
                                   id UUID PRIMARY KEY,
                                   crn TEXT NOT NULL,
                                   first_name TEXT NOT NULL,
                                   last_name TEXT NOT NULL,
                                   date_of_birth DATE NOT NULL,
                                   region TEXT NOT NULL,
                                   email TEXT NOT NULL,
                                   course_name TEXT NOT NULL,
                                   course_type TEXT NOT NULL,
                                   provider TEXT NOT NULL,
                                   status TEXT NOT NULL,
                                   total_time BIGINT NOT NULL,
                                   expected_minutes INTEGER NOT NULL,
                                   external_id TEXT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add index on commonly queried fields
CREATE INDEX idx_ete_course_events_crn ON ete_course_events(crn);
CREATE INDEX idx_ete_course_events_external_id ON ete_course_events(external_id);
CREATE INDEX idx_ete_course_events_created_at ON ete_course_events(created_at);