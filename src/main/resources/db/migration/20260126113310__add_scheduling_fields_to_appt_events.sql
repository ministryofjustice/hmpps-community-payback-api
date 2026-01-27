ALTER TABLE appointment_events
    ADD allocation_id bigint NULL,
    ADD triggered_by_scheduling_id uuid NULL,
    ALTER COLUMN delius_version_to_update DROP NOT NULL,
    ALTER COLUMN supervisor_officer_code DROP NOT NULL;
