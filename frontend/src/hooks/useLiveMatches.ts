import { useInfiniteQuery } from "@tanstack/react-query"
import { gqlNoJwt } from "@/lib/graphql"
import type { SpectateMatchPage } from "@/lib/playerServiceTypes/SpectateMatch"

const LIVE_MATCHES_QUERY = `
    query LiveMatches($afterCursor: String, $size: Int) {
      liveMatches(afterCursor: $afterCursor, size: $size) {
        matches {
          matchId
          status
          serverId
          websocketUrl
          whitePlayer { playerId displayName rating type }
          blackPlayer { playerId displayName rating type }
          whiteTimeSeconds
          blackTimeSeconds
          lastFen
          activeColor
          moveNumber
          winnerId
          reason
          updatedAt
          botConfiguration { botModelType botMoveTimeModelType }
          matchType
          timeControl
        }
        nextCursor
        hasMore
      }
    }
  `

export function useLiveMatches(pageSize = 20) {
  return useInfiniteQuery({
    queryKey: ["liveMatches"],
    queryFn: ({ pageParam }) =>
      gqlNoJwt<{ liveMatches: SpectateMatchPage }>(LIVE_MATCHES_QUERY, {
        afterCursor: pageParam ?? null,
        size: pageSize,
      }).then((d) => d.liveMatches),
    initialPageParam: null as string | null,
    getNextPageParam: (page) => (page.hasMore ? page.nextCursor : undefined),
    refetchInterval: 5_000,
    refetchIntervalInBackground: false,
  })
}
