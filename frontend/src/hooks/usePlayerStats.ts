import { useQuery } from "@tanstack/react-query"
import { gqlNoJwt } from "@/lib/graphql"
import type {
  PlayerRating,
  PlayerStats,
} from "@/lib/playerServiceTypes/PlayerStats"

const DEFAULT_PLAYER_STATS_QUERY = `
    query PlayerStats($playerId: String!) {
      playerStats(playerId: $playerId) {
        playerId
        displayName
        skillRating
        wins
        losses
        draws
        totalGames
        winRate
        currentStreak
        longestWinStreak
        favouriteTimeControl
        memberSinceEpochMs
        winsAsWhite
        winsAsBlack
      }
    }
  `

const PLAYER_RATING_QUERY = `
    query PlayerStats($playerId: String!) {
      playerStats(playerId: $playerId) {
        playerId
        skillRating
      }
    }
  `

export function usePlayerRating(playerId: string | null) {
  return usePlayerStats<PlayerRating>(
    playerId,
    PLAYER_RATING_QUERY,
    "playerRating"
  )
}

export function usePlayerStats<QueryReturnType = PlayerStats>(
  playerId: string | null,
  query: string = DEFAULT_PLAYER_STATS_QUERY,
  queryKey: string = "playerStats"
) {
  return useQuery({
    queryKey: [queryKey, playerId],
    queryFn: () =>
      gqlNoJwt<{ playerStats: QueryReturnType }>(query, { playerId }).then(
        (d) => d.playerStats
      ),
    enabled: !!playerId, // won't run if no playerId
    staleTime: 5 * 60_000, // matches the 5min server cache TTL
  })
}
