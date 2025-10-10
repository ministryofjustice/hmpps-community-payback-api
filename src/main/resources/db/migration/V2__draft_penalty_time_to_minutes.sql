-- V2__change_penalty_time_to_integer.sql
ALTER TABLE appointment_drafts
    ALTER COLUMN penalty_time TYPE INTEGER
        USING EXTRACT(HOUR FROM penalty_time) * 60 + EXTRACT(MINUTE FROM penalty_time);