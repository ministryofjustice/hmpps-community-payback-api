ALTER TABLE adjustment_events ADD COLUMN referenced_event_id UUID NULL;
ALTER TABLE adjustment_events ADD FOREIGN KEY (referenced_event_id) REFERENCES adjustment_events(id);
