import { useQuery } from "@tanstack/react-query"
import { gqlNoJwt } from "@/lib/graphql"
import type { SpectateMatch } from "@/lib/playerServiceTypes/SpectateMatch"

const SPECTATE_MATCH_QUERY = `
    query SpectateMatch($matchId: ID!) {
      spectateMatch(matchId: $matchId) {
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
    }
  `

export function useSpectateMatch(matchId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: ["spectateMatch", matchId],
    queryFn: () =>
      gqlNoJwt<{ spectateMatch: SpectateMatch | null }>(SPECTATE_MATCH_QUERY, {
        matchId,
      }).then((d) => d.spectateMatch),
    enabled: enabled && !!matchId,
  })
}
