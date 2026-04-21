import type { PieceType } from "@/types/Chess.type"

export type MoveMessage = {
  matchId: String
  playerId: String
  from: String // e.g., "e2"
  to: String // e.g., "e4"
  promotion?: PieceType
}
