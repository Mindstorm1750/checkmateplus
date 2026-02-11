
export type MatchStateMessage = {
    matchId: String,
    fen: String,
    whitePlayerTime: number,
    blackPlayerTime: number,
    isCheck: Boolean,
    isCheckmate: Boolean,
    isStalemate: Boolean,
    isDraw: Boolean,
    lastMove?: MoveNotification,
    whitePlayer?: Player,
    blackPlayer?: Player
}

export type MoveNotification = {
    matchId: String,
    from: String,
    to: String,
    promotion?: String,
    movedBy: String,
}

export type Player = {
    displayName: String,
    playerId: String,
    skillRating?: number,
}