package playerModels

import kotlinx.serialization.Serializable

/**
 * Match lifecycle status
 */
@Serializable
enum class MatchStatus {
    PENDING,      // Match created, waiting for game server assignment
    ASSIGNED,     // Game server assigned, ready to start
    ACTIVE,       // Game in progress (updated by game server)
    COMPLETED,    // Game finished
    CANCELLED,    // Match cancelled (timeout, player disconnect before start)
    ABANDONED     // Player disconnected during game
}