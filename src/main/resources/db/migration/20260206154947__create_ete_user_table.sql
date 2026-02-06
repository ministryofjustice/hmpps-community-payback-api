-- Create the ete_user table if it doesn't exist
CREATE TABLE IF NOT EXISTS ete_user (
                                        id UUID PRIMARY KEY,
                                        crn VARCHAR(255) NOT NULL,
                                        email VARCHAR(255) NOT NULL,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT uk_ete_user_crn_email UNIQUE (crn, email)
);

-- Create indexes for performance if they don't exist
CREATE INDEX IF NOT EXISTS idx_ete_user_crn ON ete_user(crn);
CREATE INDEX IF NOT EXISTS idx_ete_user_email_address ON ete_user(email);

-- Drop the old many-to-many join table if it exists
DROP TABLE IF EXISTS ete_user_course_completion;

-- Add user_id column to ete_course_completion_events table for the many-to-one relationship
ALTER TABLE ete_course_completion_events
    ADD COLUMN IF NOT EXISTS user_id UUID;

-- Create foreign key constraint for the many-to-one relationship
ALTER TABLE ete_course_completion_events
    ADD CONSTRAINT fk_course_completion_user
        FOREIGN KEY (user_id)
            REFERENCES ete_user (id);

-- Create index for performance on the new foreign key
CREATE INDEX IF NOT EXISTS idx_course_completion_user_id ON ete_course_completion_events(user_id);