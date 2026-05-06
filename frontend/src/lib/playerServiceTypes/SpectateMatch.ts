import type { MatchType } from "../stompMessageTypes/ChessLibrary/PlayerModels/MatchType"
import type { TimeControl } from "../stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"

export type SpectatePlayerType = "REGISTERED" | "BOT" | "ANONYMOUS"

export type SpectatePlayer = {
  playerId: string
  displayName: string
  rating: number
  type: SpectatePlayerType
}

export type SpectateBotConfiguration = {
  botModelType: string
  botMoveTimeModelType: string
}

export type SpectateMatch = {
  matchId: string
  status: MatchStatus
  serverId?: string
  websocketUrl?: string
  whitePlayer?: SpectatePlayer
  blackPlayer?: SpectatePlayer
  whiteTimeSeconds: number
  blackTimeSeconds: number
  lastFen?: string
  activeColor?: string
  moveNumber: number
  winnerId?: string
  reason?: string
  updatedAt: number
  botConfiguration?: SpectateBotConfiguration
  matchType?: MatchType
  timeControl?: TimeControl
}

export type SpectateMatchPage = {
  matches: Array<SpectateMatch>
  nextCursor?: string
  hasMore: boolean
}

export type MatchStatus =
  | "PENDING"
  | "ASSIGNED"
  | "ACTIVE"
  | "COMPLETED"
  | "CANCELLED"
  | "ABANDONED"
