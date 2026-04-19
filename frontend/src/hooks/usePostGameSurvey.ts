import { useMutation } from "@tanstack/react-query"
import { gql } from "@/lib/graphql"

const POST_GAME_SURVEY_MUTATION = `
  mutation PostGameSurvey($surveyScore: Int!, $optionalComments: String, $matchId: String!) {
    postGameSurvey(surveyScore: $surveyScore, optionalComments: $optionalComments, matchId: $matchId) {
      surveyScore
      optionalComments
      matchId
    }
  }
`

export type SurveyInput = {
  surveyScore: number
  optionalComments?: string
  matchId: string
}

export function usePostGameSurvey() {
  return useMutation({
    mutationFn: (input: SurveyInput) =>
      gql<{ postGameSurvey: SurveyInput }>(POST_GAME_SURVEY_MUTATION, {
        ...input,
      }),
  })
}
