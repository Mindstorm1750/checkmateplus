import { useQuery } from "@tanstack/react-query"
import { gql } from "@/lib/graphql"
import type { MoveAnalysis } from "@/components/match-analysis"

export type AnalysisStatus =
  | "PENDING"
  | "QUEUED"
  | "RUNNING"
  | "COMPLETE"
  | "FAILED"
  | "DISABLED"

export type MatchAnalysisResponse = {
  matchId: string
  status: AnalysisStatus
  moves: MoveAnalysis[]
}

const MATCH_ANALYSIS_QUERY = `
  query MatchAnalysis($matchId: ID!) {
    matchAnalysis(matchId: $matchId) {
      matchId
      status
      moves {
        ply
        moveUci
        bestMoveUci
        scoreBefore { cp mate }
        scoreAfter { cp mate }
        cpLoss
        classification
        pv
        depth
      }
    }
  }
`

export function useMatchAnalysis(matchId: string | null | undefined) {
  return useQuery({
    queryKey: ["matchAnalysis", matchId],
    queryFn: () =>
      gql<{ matchAnalysis: MatchAnalysisResponse | null }>(
        MATCH_ANALYSIS_QUERY,
        { matchId }
      ).then((d) => d.matchAnalysis),
    enabled: !!matchId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === "QUEUED" || status === "RUNNING" ? 4000 : false
    },
  })
}
