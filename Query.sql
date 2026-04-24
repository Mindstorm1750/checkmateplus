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