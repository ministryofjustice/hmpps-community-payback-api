-- Make contact_outcome_id nullable on appointment_outcomes
ALTER TABLE appointment_outcomes
  ALTER COLUMN contact_outcome_id DROP NOT NULL;
