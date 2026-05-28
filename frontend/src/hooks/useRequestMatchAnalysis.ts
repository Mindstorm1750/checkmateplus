import { useMutation, useQueryClient } from "@tanstack/react-query"
import { gql } from "@/lib/graphql"
import type { MatchAnalysisResponse } from "./useMatchAnalysis"

const REQUEST_MATCH_ANALYSIS_MUTATION = `
  mutation RequestMatchAnalysis($matchId: ID!) {
    requestMatchAnalysis(matchId: $matchId) {
      matchId
      status
    }
  }
`

export function useRequestMatchAnalysis() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: string) =>
      gql<{ requestMatchAnalysis: MatchAnalysisResponse }>(
        REQUEST_MATCH_ANALYSIS_MUTATION,
        { matchId }
      ).then((d) => d.requestMatchAnalysis),
    onSuccess: (_data, matchId) => {
      queryClient.invalidateQueries({ queryKey: ["matchAnalysis", matchId] })
    },
  })
}
