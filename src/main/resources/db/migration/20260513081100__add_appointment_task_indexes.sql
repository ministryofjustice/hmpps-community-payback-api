CREATE INDEX appointment_tasks_task_status_created_at_idx ON appointment_tasks (task_status, created_at);
CREATE INDEX appointments_provider_code_date_idx ON appointments (provider_code, date);
CREATE INDEX appointments_date_idx ON appointments (date);
