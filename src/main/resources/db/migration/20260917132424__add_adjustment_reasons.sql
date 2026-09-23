INSERT INTO adjustment_reasons (id, delius_code, name, max_minutes_allowed, needs_link_to_appointment)
VALUES
    (gen_random_uuid(), 'A', 'Enforcement Action', 180, FALSE),
    (gen_random_uuid(), 'H', 'Engagement completed, hours credited', 180, FALSE),
    (gen_random_uuid(), 'S', 'LPT CP Induction Hours', 180, FALSE),
    (gen_random_uuid(), 'E', 'Miscellaneous Correction', 180, FALSE),
    (gen_random_uuid(), 'B', 'Transfer from Another Order', 180, FALSE),
    (gen_random_uuid(), 'D', 'Transfer from Another Provider', 180, FALSE),
    (gen_random_uuid(), 'C', 'Transfer to Another Order', 180, FALSE);
