import { useInfiniteQuery } from "@tanstack/react-query"
import { gql } from "@/lib/graphql"
import type { MatchHistoryPage } from "@/lib/playerServiceTypes/MatchHistoryPage"

const MATCH_HISTORY_QUERY = `
    query MatchHistory($playerId: String!, $afterCursor: String, $size: Int) {
      matchHistory(playerId: $playerId, afterCursor: $afterCursor, size: $size) {
        matches {
          matchId
          opponentId
          result
          colour
          matchType
          timeControl
          gameDurationMs
          createdAtEpochMs
        }
        nextCursor
        hasMore
      }
    }
  `

export function useMatchHistory(
  playerId: string | null | undefined,
  pageSize = 20
) {
  return useInfiniteQuery({
    queryKey: ["matchHistory", playerId],
    queryFn: ({ pageParam }) =>
      gql<{ matchHistory: MatchHistoryPage }>(MATCH_HISTORY_QUERY, {
        playerId,
        afterCursor: pageParam ?? null,
        size: pageSize,
      }).then((d) => d.matchHistory),
    initialPageParam: null as string | null,
    getNextPageParam: (page) => (page.hasMore ? page.nextCursor : undefined),
    enabled: !!playerId,
  })
}
