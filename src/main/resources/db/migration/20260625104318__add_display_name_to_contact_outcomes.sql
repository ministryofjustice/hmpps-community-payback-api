ALTER TABLE contact_outcomes
ADD COLUMN display_name TEXT;

UPDATE contact_outcomes
SET display_name = t.display_name
FROM (
    VALUES
        ('Acceptable Absence - Stood Down', E'Acceptable absence \u2013 stood down'),
        ('Attended - Complied', E'Attended \u2013 complied'),
        ('Attended - Failed to Comply', E'Attended \u2013 failed to comply'),
        ('Attended - Sent Home (behaviour)', E'Attended \u2013 sent home (behaviour)'),
        ('Attended - Sent Home (service issues)', E'Attended \u2013 sent home (service issues)'),
        ('Rescheduled - PoP Request', E'Rescheduled \u2013 person on probation request'),
        ('Rescheduled - Service Request', E'Rescheduled \u2013 service request'),
        ('Unacceptable Absence', E'Unacceptable absence')
) AS t(name, display_name)
WHERE contact_outcomes.name = t.name;
