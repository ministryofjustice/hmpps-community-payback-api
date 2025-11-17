-- Deletes duplicate rows for code 'CO05', keeping only one

DELETE FROM contact_outcomes
WHERE code = 'CO05'
  AND id NOT IN (
    SELECT id
    FROM contact_outcomes
    WHERE code = 'CO05'
    ORDER BY id
    LIMIT 1
);

-- Ensure unique values for the `code` column on contact_outcomes
-- Postgres will create an index to enforce this constraint

ALTER TABLE contact_outcomes
  ADD CONSTRAINT contact_outcomes_code_key UNIQUE (code);
