package uk.ac.kent.gameserver.models.messages

enum class MatchEventType {
    PLAYER_JOINED,
    PLAYER_LEFT,
    MATCH_STARTED,
    MATCH_ENDED,
    DRAW_OFFERED,
    DRAW_ACCEPTED,
    DRAW_DECLINED,
    PLAYER_RESIGNED,
    TIME_EXPIRED,
    RECONNECTED,
    DISCONNECTED,
    ILLEGAL_MOVE
}
