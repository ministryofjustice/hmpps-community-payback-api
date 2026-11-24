ALTER TABLE adjustments ADD CONSTRAINT adjustments_delius_id_unique UNIQUE (delius_id);

CREATE INDEX adjustment_events_delius_adjustment_id_event_type_idx
    ON adjustment_events (delius_adjustment_id, event_type);
