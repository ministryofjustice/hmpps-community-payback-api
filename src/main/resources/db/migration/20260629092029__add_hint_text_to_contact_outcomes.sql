ALTER TABLE contact_outcomes
ADD COLUMN hint_text TEXT;

UPDATE contact_outcomes
SET hint_text = t.hint_text
FROM (
    VALUES
        ('Acceptable Absence - Stood Down', 'Usually preplanned before the session'),
        ('Attended - Concurrent Hours', 'If the person on probation is completing hours towards more than one sentence requirement'),
        ('Attended - Failed to Comply', 'For example repeated lateness, arriving unfit or ill, leaving early without evidence, not wearing the right protective equipment'),
        ('Attended - Sent Home (behaviour)', 'For example aggression, substance influence, refusal to comply with instructions, unsafe behaviour'),
        ('Attended - Sent Home (service issues)', 'For example oversubscribed, supervisor illness, early session closure and vehicle, weather or operational issues'),
        ('Rescheduled - PoP Request', 'For example previously agreed appointments or caring responsibilities'),
        ('Rescheduled - Service Request', 'Usually preplanned before the session'),
        ('Unacceptable Absence', 'No valid reason given')
) AS t(name, hint_text)
WHERE contact_outcomes.name = t.name;
