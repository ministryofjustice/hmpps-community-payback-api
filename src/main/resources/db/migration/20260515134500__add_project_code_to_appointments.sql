ALTER TABLE appointments ADD COLUMN project_code TEXT;

UPDATE appointments a
SET project_code = (
    SELECT ae.project_code
    FROM appointment_events ae
    WHERE ae.appointment_id = a.id
    ORDER BY ae.triggered_at DESC
    LIMIT 1
)
WHERE a.project_code IS NULL;

ALTER TABLE appointments ALTER COLUMN project_code SET NOT NULL;
