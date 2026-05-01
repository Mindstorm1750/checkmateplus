import type { PieceType } from "@/types/Chess.type"

export type MoveMessage = {
  matchId: string
  playerId: string
  from: string // e.g., "e2"
  to: string // e.g., "e4"
  promotion?: PieceType
}
