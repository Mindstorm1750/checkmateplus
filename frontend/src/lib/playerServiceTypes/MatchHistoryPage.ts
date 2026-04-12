import type { MatchSummary } from "./MatchSummary"

export type MatchHistoryPage = {
    matches: Array<MatchSummary>,
    nextCursor?: string,
    hasMore: Boolean
}