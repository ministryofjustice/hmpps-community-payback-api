-- Deletes all course completions that have not been resolved by a case admin after a week.

BEGIN;

-- Clean up draft resolutions for CCs
WITH course_completions_to_delete AS (
  SELECT ev.id
  FROM ete_course_completion_events ev
  LEFT JOIN ete_course_completion_event_resolutions er
    ON ev.id = er.ete_course_completion_event_id
  WHERE
    er.id IS NULL
    AND ev.received_at < current_timestamp - interval '7 days'
)
DELETE FROM ete_course_completion_draft_resolutions
WHERE ete_course_completion_event_id IN (SELECT id FROM course_completions_to_delete);

-- Clean up the CCs themselves
WITH course_completions_to_delete AS (
  SELECT ev.id
  FROM ete_course_completion_events ev
  LEFT JOIN ete_course_completion_event_resolutions er
    ON ev.id = er.ete_course_completion_event_id
  WHERE
    er.id IS NULL
    AND ev.received_at < current_timestamp - interval '7 days'
)
DELETE FROM ete_course_completion_events
WHERE id IN (SELECT id FROM course_completions_to_delete);

COMMIT;
