-- Reversing typo fix for Birmingham Central to restore double space as this is what is received from community campus
UPDATE community_campus_pdus
SET name = 'Birmingham  Central'
WHERE name = 'Birmingham Central';
