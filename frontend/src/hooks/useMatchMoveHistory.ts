import { useQuery } from "@tanstack/react-query"
import { gqlNoJwt } from "@/lib/graphql"
import type { TimeControl } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType"
import type { SpectatePlayer } from "@/lib/playerServiceTypes/SpectateMatch"
import type { MoveHistoryEntry } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"

export type MatchOutcome = "WHITE_WINS" | "BLACK_WINS" | "DRAW" | "ABANDONED"

export type MatchMoveHistory = {
  matchId: string
  whitePlayer: SpectatePlayer | null
  blackPlayer: SpectatePlayer | null
  timeControl: TimeControl
  matchType: MatchType
  outcome: MatchOutcome
  finalFen: string
  moves: MoveHistoryEntry[]
  createdAtEpochMs: number
  gameDurationMs: number
}

const MATCH_MOVE_HISTORY_QUERY = `
  query MatchMoveHistory($matchId: ID!) {
    matchMoveHistory(matchId: $matchId) {
      matchId
      whitePlayer { playerId displayName rating type }
      blackPlayer { playerId displayName rating type }
      timeControl
      matchType
      outcome
      finalFen
      moves { san from to fenAfter colour playerId }
      createdAtEpochMs
      gameDurationMs
    }
  }
`

export function useMatchMoveHistory(matchId: string | null | undefined) {
  return useQuery({
    queryKey: ["matchMoveHistory", matchId],
    queryFn: () =>
      gqlNoJwt<{ matchMoveHistory: MatchMoveHistory | null }>(
        MATCH_MOVE_HISTORY_QUERY,
        { matchId }
      ).then((d) => d.matchMoveHistory),
    enabled: !!matchId,
  })
}
