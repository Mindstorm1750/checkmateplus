ALTER TABLE match_records
    ADD remaining_black_time BIGINT;

ALTER TABLE match_records
    ADD remaining_white_time BIGINT;

ALTER TABLE match_records
    ALTER COLUMN remaining_black_time SET NOT NULL;

ALTER TABLE match_records
    ALTER COLUMN remaining_white_time SET NOT NULL;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_account UNIQUE (account_id);

ALTER TABLE match_records
    DROP COLUMN remaining_black_time_ms;

ALTER TABLE match_records
    DROP COLUMN remaining_white_time_ms;

ALTER TABLE players
    ALTER COLUMN difficulty SET NOT NULL;