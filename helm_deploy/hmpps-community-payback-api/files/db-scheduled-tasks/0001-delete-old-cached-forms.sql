-- Deletes cached form data that has expired.

DELETE FROM form_cache
WHERE updated_at < current_timestamp - interval '7 days';
