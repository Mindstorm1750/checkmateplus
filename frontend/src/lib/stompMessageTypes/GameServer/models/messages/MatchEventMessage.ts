export type MatchEventMessage = {
  matchId: String
  eventType: MatchEventType
  message: String
  data?: Map<String, any>
  timestamp: number
}

export type MatchEventType =
  | "PLAYER_JOINED"
  | "PLAYER_LEFT"
  | "MATCH_STARTED"
  | "MATCH_ENDED"
  | "DRAW_OFFERED"
  | "DRAW_ACCEPTED"
  | "DRAW_DECLINED"
  | "PLAYER_RESIGNED"
  | "TIME_EXPIRED"
  | "RECONNECTED"
  | "DISCONNECTED"
  | "ILLEGAL_MOVE"

export const GAME_OVER_TITLES: Partial<Record<MatchEventType, string>> = {
  MATCH_ENDED: "Game ended",
  TIME_EXPIRED: "Game ended by time-out",
  PLAYER_RESIGNED: "Game ended by resignation",
  DRAW_ACCEPTED: "Game ended by draw agreement",
}
