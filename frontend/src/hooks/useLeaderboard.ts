import { useQuery } from '@tanstack/react-query'
import { gqlNoJwt } from '@/lib/graphql'
import type { LeaderboardEntry } from '@/lib/playerServiceTypes/LeaderboardEntry'

const LEADERBOARD_QUERY = `
    query Leaderboard($size: Int) {
      leaderboard(size: $size) {
        rank
        playerId
        displayName
        skillRating
      }
    }
  `

export function useLeaderboard(size = 10) {
    return useQuery({
        queryKey: ['leaderboard', size],
        queryFn: () =>
            gqlNoJwt<{ leaderboard: LeaderboardEntry[] }>(LEADERBOARD_QUERY, { size })
                .then(d => d.leaderboard),
        staleTime: 60_000, // matches the 60s server cache TTL
    })
}
