ALTER TABLE match_records
    ADD COLUMN analysis_json JSONB;

ALTER TABLE match_records
    ADD COLUMN analysis_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
