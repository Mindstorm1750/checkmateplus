    package org.checkmateplus.chessLibrary.playerModels

    /**
     * Represents the type of queue a player can join in the matchmaking service.
     */
    enum class MatchType {
        /** Casual player versus player match. */
        CASUAL_PVP,

        /** Casual player versus AI match (e.g., against a bot). */
        PVE,

        /** Competitive player versus player match. */
        COMPETITIVE_PVP,
    }