
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
                                              CONSTRAINT pk_refresh_tokens        PRIMARY KEY (token),
                                              CONSTRAINT fk_refresh_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (account_id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_account ON refresh_tokens (account_id);

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

-- surveys (post-match feedback linked to match_records)
CREATE TABLE IF NOT EXISTS surveys (
                                       survey_id         VARCHAR(255) NOT NULL,
                                       match_id          VARCHAR(255) NOT NULL,
                                       survey_score      INTEGER      NOT NULL,
                                       optional_comments TEXT,
                                       CONSTRAINT pk_surveys       PRIMARY KEY (survey_id),
                                       CONSTRAINT fk_surveys_match FOREIGN KEY (match_id) REFERENCES match_records (match_id)
);

CREATE INDEX IF NOT EXISTS idx_surveys_match ON surveys (match_id);

-- Players
          INSERT INTO players (player_id, player_type, display_name, joined_queue_at, skill_rating, rating_deviation, volatility, player_preferences, difficulty, bot_configuration)
          VALUES
              ('player-001', 'REGISTERED', 'Magnus', 1700000000000, 1800, 45.0, 0.06, '{"boardColour":"green"}', NULL, NULL),
              ('player-002', 'REGISTERED', 'Hikaru', 1700000000001, 1650, 60.0, 0.07, '{"boardColour":"green"}', NULL, NULL),
              ('player-003', 'REGISTERED', 'GarryK', 1700000000002, 1500, 80.0, 0.08, '{"boardColour":"green"}', NULL, NULL),
              ('player-004', 'ANONYMOUS',  'Guest_7842', 1700000000003, NULL, NULL, NULL, NULL, NULL, NULL),
              ('player-005', 'BOT',        'StockfishBot', 1700000000004, 2200, 30.0, 0.05, NULL, 8, '{"engine":"MINMAX"}');

-- Accounts (only REGISTERED players have accounts)
          INSERT INTO accounts (account_id, username, totp_secret, totp_enabled, google_id, time_registered, player_id)
          VALUES
              ('account-001', 'magnus',  'LPIKT35V663FUZU5MZQUYKNWIU3YPY5NHHR644P3CXNIBZ54UAKV2SWBAPBND2X4', TRUE, NULL,           1700000000000, 'player-001'),
              ('account-002', 'hikaru',  'LPIKT35V663FUZU5MZQUYKNWIU3YPY5NHHR644P3CXNIBZ54UAKV2SWBAPBND2X4', TRUE, 'google-hik-1', 1700000000001, 'player-002'),
              ('account-003', 'garrykk', 'LPIKT35V663FUZU5MZQUYKNWIU3YPY5NHHR644P3CXNIBZ54UAKV2SWBAPBND2X4', TRUE, NULL,           1700000000002, 'player-003');