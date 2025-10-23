ALTER TABLE appointment_outcomes ADD delius_version_to_update UUID NOT NULL;
ALTER TABLE appointment_outcomes ADD sensitive boolean NOT NULL;
ALTER TABLE appointment_outcomes ADD alert_active boolean NOT NULL;
