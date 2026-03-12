
          -- players (single-table inheritance: REGISTERED, ANONYMOUS, BOT)
CREATE TABLE IF NOT EXISTS players (
                                       player_id          VARCHAR(255)       NOT NULL,
                                       player_type        VARCHAR(31)        NOT NULL,
                                       display_name       VARCHAR(255)       NOT NULL,
                                       joined_queue_at    BIGINT             NOT NULL,
                                       skill_rating       INTEGER,
                                       rating_deviation   DOUBLE PRECISION,
                                       volatility         DOUBLE PRECISION,
                                       player_preferences JSONB,
                                       difficulty         INTEGER,
                                       bot_configuration  JSONB,
                                       CONSTRAINT pk_players PRIMARY KEY (player_id)
);

-- accounts
CREATE TABLE IF NOT EXISTS accounts (
                                        account_id        VARCHAR(255) NOT NULL,
                                        username          VARCHAR(255) NOT NULL,
                                        totp_secret       VARCHAR(255),
                                        totp_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
                                        google_id         VARCHAR(255),
                                        time_registered   BIGINT,
                                        player_id         VARCHAR(255) NOT NULL,
                                        CONSTRAINT pk_accounts          PRIMARY KEY (account_id),
                                        CONSTRAINT uq_accounts_username UNIQUE (username),
                                        CONSTRAINT uq_accounts_google   UNIQUE (google_id),
                                        CONSTRAINT uq_accounts_player   UNIQUE (player_id),
                                        CONSTRAINT fk_accounts_player   FOREIGN KEY (player_id) REFERENCES players (player_id)
);

-- refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              token      VARCHAR(255) NOT NULL,
                                              account_id VARCHAR(255) NOT NULL,
                                              expires_at BIGINT       NOT NULL,
                                              revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
                                              CONSTRAINT pk_refresh_tokens PRIMARY KEY (token)
);

-- match_records
-- Note: createdAtEpochMs uses a camelCase @Column name; PostgreSQL folds it to createdatepochms.
CREATE TABLE IF NOT EXISTS match_records (
                                             match_id                  VARCHAR(255) NOT NULL,
                                             board_state               JSONB,
                                             time_control              VARCHAR(50)  NOT NULL,
                                             match_type                VARCHAR(50)  NOT NULL,
                                             bot_configuration         JSONB,
                                             match_outcome             VARCHAR(50)  NOT NULL,
                                             white_player_id           VARCHAR(255) NOT NULL,
                                             black_player_id           VARCHAR(255) NOT NULL,
                                             remaining_white_time_ms   BIGINT       NOT NULL,
                                             remaining_black_time_ms   BIGINT       NOT NULL,
                                             match_status              VARCHAR(50)  NOT NULL,
                                             match_history             JSONB        DEFAULT '[]',
                                             createdatepochms          BIGINT       NOT NULL,
                                             CONSTRAINT pk_match_records       PRIMARY KEY (match_id),
                                             CONSTRAINT fk_match_white_player  FOREIGN KEY (white_player_id) REFERENCES players (player_id),
                                             CONSTRAINT fk_match_black_player  FOREIGN KEY (black_player_id) REFERENCES players (player_id)
);

CREATE INDEX IF NOT EXISTS idx_match_records_white ON match_records (white_player_id);
CREATE INDEX IF NOT EXISTS idx_match_records_black ON match_records (black_player_id);