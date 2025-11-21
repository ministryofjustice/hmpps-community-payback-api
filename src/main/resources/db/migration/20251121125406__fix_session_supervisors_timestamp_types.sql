delete from session_supervisors;

ALTER TABLE session_supervisors ALTER COLUMN created_at TYPE timestamptz USING created_at::timestamptz;
ALTER TABLE session_supervisors ALTER COLUMN updated_at TYPE timestamptz USING updated_at::timestamptz;
