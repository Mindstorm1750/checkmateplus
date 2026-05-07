import type { SpectateMatch } from "@/lib/playerServiceTypes/SpectateMatch.ts"

/**
 * Filter for matches that are active and that our user isn't a part of
 */
export function spectateMatchFilter(
  spectateMatches: SpectateMatch[],
  playerId: string | null
) {
  return spectateMatches
    .filter((match) => match.status == "ACTIVE")
    .filter(
      (match) =>
        playerId != match.whitePlayer?.playerId &&
        playerId != match.blackPlayer?.playerId
    )
}
