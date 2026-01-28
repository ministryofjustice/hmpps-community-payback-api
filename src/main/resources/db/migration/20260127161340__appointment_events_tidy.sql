-- we only currently have test data in this table
DROP TABLE appointment_events;

CREATE TABLE appointment_events (
   id uuid NOT NULL,
   event_type text NOT NULL,
   created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
   trigger_type text NULL,
   triggered_by text NULL,

   triggered_scheduling_at timestamptz NULL,
   triggered_scheduling_id uuid NULL,

   crn varchar NOT NULL,
   delius_event_number int8 NOT NULL,
   community_payback_appointment_id uuid NULL,
   delius_appointment_id int8 NOT NULL,
   prior_delius_version uuid NULL,
   delius_allocation_id int8 NULL,

   project_code text NOT NULL,
   "date" date NOT NULL,
   start_time time NOT NULL,
   end_time time NOT NULL,
   pickup_location_code text NULL,
   pickup_time time NULL,
   supervisor_officer_code text NULL,
   notes text NULL,

   contact_outcome_id uuid NULL,
   hi_vis_worn bool NULL,
   worked_intensively bool NULL,
   penalty_minutes int8 NULL,
   minutes_credited int8 NULL,
   work_quality text NULL,
   behaviour text NULL,
   "sensitive" bool NULL,
   alert_active bool NULL,

   CONSTRAINT appointment_outcomes_behaviour_check CHECK ((behaviour = ANY (ARRAY['EXCELLENT'::text, 'GOOD'::text, 'NOT_APPLICABLE'::text, 'POOR'::text, 'SATISFACTORY'::text, 'UNSATISFACTORY'::text]))),
   CONSTRAINT appointment_outcomes_penalty_minutes_check CHECK ((penalty_minutes >= 0)),
   CONSTRAINT appointment_outcomes_pkey PRIMARY KEY (id),
   CONSTRAINT appointment_outcomes_work_quality_check CHECK ((work_quality = ANY (ARRAY['EXCELLENT'::text, 'GOOD'::text, 'NOT_APPLICABLE'::text, 'POOR'::text, 'SATISFACTORY'::text, 'UNSATISFACTORY'::text])))
);

ALTER TABLE appointment_events ADD CONSTRAINT appointment_outcomes_contact_outcome_id_fkey FOREIGN KEY (contact_outcome_id) REFERENCES contact_outcomes(id);