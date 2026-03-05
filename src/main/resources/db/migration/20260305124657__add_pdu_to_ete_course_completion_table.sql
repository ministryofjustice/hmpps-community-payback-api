-- this is safe because the table isn't currently in active use
DELETE FROM ete_course_completion_events;

ALTER TABLE ete_course_completion_events ADD COLUMN community_campus_pdu_id UUID NOT NULL;

ALTER TABLE ete_course_completion_events ADD CONSTRAINT ete_course_completion_event_community_campus_pdu_id_fk FOREIGN KEY (community_campus_pdu_id) REFERENCES community_campus_pdus(id);