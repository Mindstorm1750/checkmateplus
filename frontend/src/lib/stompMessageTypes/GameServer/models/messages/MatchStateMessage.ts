import type { PieceType } from "@/types/Chess.type"

export type MatchStateMessage = {
  matchId: string
  fen: string
  whitePlayerTime: number
  blackPlayerTime: number
  isCheck: boolean
  isCheckmate: boolean
  isStalemate: boolean
  isDraw: boolean
  lastMove?: MoveNotification
  whitePlayer?: Player
  blackPlayer?: Player
}

export type MoveNotification = {
  matchId: string
  from: string
  to: string
  promotion?: PieceType
  movedBy: string
}

export type Player = {
  displayName: string
  playerId: string
  skillRating?: number
}
