ALTER TABLE match_records
    RENAME COLUMN remaining_white_time_ms TO remaining_white_time;

ALTER TABLE match_records
    RENAME COLUMN remaining_black_time_ms TO remaining_black_time;

ALTER TABLE match_records
    ALTER COLUMN remaining_black_time SET NOT NULL;

ALTER TABLE match_records
    ALTER COLUMN remaining_white_time SET NOT NULL;