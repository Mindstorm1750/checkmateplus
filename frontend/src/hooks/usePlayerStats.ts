import { useQuery } from "@tanstack/react-query"
import { gql } from "@/lib/graphql"
import type { PlayerStats } from "@/lib/playerServiceTypes/PlayerStats"

const PLAYER_STATS_QUERY = `
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
      }
    }
  `

export function usePlayerStats(playerId: string | null) {
  return useQuery({
    queryKey: ["playerStats", playerId],
    queryFn: () =>
      gql<{ playerStats: PlayerStats }>(PLAYER_STATS_QUERY, { playerId }).then(
        (d) => d.playerStats
      ),
    enabled: !!playerId, // won't run if no playerId
    staleTime: 5 * 60_000, // matches the 5min server cache TTL
  })
}
