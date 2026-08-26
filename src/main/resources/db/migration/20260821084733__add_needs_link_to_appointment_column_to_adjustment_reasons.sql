ALTER TABLE adjustment_reasons
ADD COLUMN needs_link_to_appointment BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE adjustment_reasons
SET needs_link_to_appointment = TRUE
WHERE delius_code = 'TTX';

ALTER TABLE adjustment_reasons
ALTER COLUMN needs_link_to_appointment DROP DEFAULT;
