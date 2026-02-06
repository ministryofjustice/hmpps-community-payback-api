ALTER TABLE appointment_events ALTER COLUMN crn TYPE text USING crn::text;

CREATE INDEX appointment_events_crn_idx ON public.appointment_events USING HASH (crn);

