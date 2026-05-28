CREATE TABLE accounts
(
    account_id      VARCHAR(255)          NOT NULL,
    username        VARCHAR(255)          NOT NULL,
    totp_secret     VARCHAR(255),
    totp_enabled    BOOLEAN DEFAULT FALSE NOT NULL,
    google_id       VARCHAR(255),
    time_registered BIGINT,
    player_id       VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

CREATE TABLE match_records
(
    match_id                VARCHAR(255) NOT NULL,
    board_state             JSONB,
    time_control            VARCHAR(50)  NOT NULL,
    match_type              VARCHAR(50)  NOT NULL,
    bot_configuration       JSONB,
    match_outcome           VARCHAR(50)  NOT NULL,
    white_player_id         VARCHAR(255) NOT NULL,
    black_player_id         VARCHAR(255) NOT NULL,
    remaining_white_time_ms BIGINT       NOT NULL,
    remaining_black_time_ms BIGINT       NOT NULL,
    match_status            VARCHAR(50)  NOT NULL,
    match_history           JSONB DEFAULT '[]',
    created_at_epoch_ms     BIGINT       NOT NULL,
    CONSTRAINT pk_match_records PRIMARY KEY (match_id)
);

CREATE TABLE players
(
    player_id          VARCHAR(255) NOT NULL,
    player_type        VARCHAR(31)  NOT NULL,
    display_name       VARCHAR(255) NOT NULL,
    joined_queue_at    BIGINT       NOT NULL,
    skill_rating       INTEGER,
    rating_deviation   DOUBLE PRECISION,
    volatility         DOUBLE PRECISION,
    player_preferences JSONB,
    difficulty         INTEGER,
    bot_configuration  JSONB,
    CONSTRAINT pk_players PRIMARY KEY (player_id)
);

CREATE TABLE refresh_tokens
(
    token      VARCHAR(255)          NOT NULL,
    account_id VARCHAR(255)          NOT NULL,
    expires_at BIGINT                NOT NULL,
    revoked    BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (token)
);

CREATE TABLE surveys
(
    survey_id         VARCHAR(255) NOT NULL,
    match_id          VARCHAR(255) NOT NULL,
    survey_score      INTEGER      NOT NULL,
    optional_comments TEXT,
    CONSTRAINT pk_surveys PRIMARY KEY (survey_id)
);

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_google UNIQUE (google_id);

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_player UNIQUE (player_id);

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_username UNIQUE (username);

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_player FOREIGN KEY (player_id) REFERENCES players (player_id) ON DELETE NO ACTION;

ALTER TABLE match_records
    ADD CONSTRAINT fk_match_black_player FOREIGN KEY (black_player_id) REFERENCES players (player_id) ON DELETE NO ACTION;

CREATE INDEX idx_match_records_black ON match_records (black_player_id);

ALTER TABLE match_records
    ADD CONSTRAINT fk_match_white_player FOREIGN KEY (white_player_id) REFERENCES players (player_id) ON DELETE NO ACTION;

CREATE INDEX idx_match_records_white ON match_records (white_player_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (account_id) ON DELETE NO ACTION;

ALTER TABLE surveys
    ADD CONSTRAINT fk_surveys_match FOREIGN KEY (match_id) REFERENCES match_records (match_id) ON DELETE NO ACTION;