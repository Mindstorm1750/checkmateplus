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

/**
 * Returns the first ACTIVE or ASSIGNED match where the given player is white or black.
 */
export function findPlayerActiveMatch(
  spectateMatches: SpectateMatch[],
  playerId: string | null
): SpectateMatch | undefined {
  if (!playerId) return undefined
  return spectateMatches.find(
    (match) =>
      (match.status === "ACTIVE" || match.status === "ASSIGNED") &&
      (match.whitePlayer?.playerId === playerId ||
        match.blackPlayer?.playerId === playerId)
  )
}
