ALTER TABLE adjustment_reasons
ADD COLUMN display_name TEXT NULL,
ADD COLUMN hint_text TEXT NULL,
ADD COLUMN rank INT NOT NULL DEFAULT 0;

UPDATE adjustment_reasons
SET
    rank = 1,
    display_name = 'Miscellaneous correction'
WHERE delius_code = 'E';

UPDATE adjustment_reasons
SET
    display_name = 'Engagement completed, hours credited',
    hint_text = 'When the person on probation has less than an hour left'
WHERE delius_code = 'H';

UPDATE adjustment_reasons
SET
    display_name = 'Transfer from another provider',
    hint_text = 'For example, Scotland, the Channel Islands, Gibraltar or internationally'
WHERE delius_code = 'D';
