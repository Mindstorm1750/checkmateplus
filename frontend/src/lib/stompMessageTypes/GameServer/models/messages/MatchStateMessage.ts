import type { PieceType } from "@/types/Chess.type"

export type MatchStateMessage = {
    matchId: String,
    fen: String,
    whitePlayerTime: number,
    blackPlayerTime: number,
    isCheck: boolean,
    isCheckmate: boolean,
    isStalemate: boolean,
    isDraw: boolean,
    lastMove?: MoveNotification,
    whitePlayer?: Player,
    blackPlayer?: Player
}

export type MoveNotification = {
  matchId: String
  from: String
  to: String
  promotion?: PieceType
  movedBy: String
}

export type Player = {
  displayName: String
  playerId: String
  skillRating?: number
}
