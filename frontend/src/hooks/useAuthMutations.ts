import { useMutation } from "@tanstack/react-query"
import { gqlNoJwt } from "@/lib/graphql"
import { LOGOUT_MUTATION } from "@/lib/auth"
import { getLocalStorage } from "@/lib/local-storage.helpers"
import type {
  ConfirmTotpResponse,
  TotpLoginResponse,
} from "@/lib/playerServiceTypes/AuthResponses"

const LOGIN_WITH_TOTP = `
  mutation LoginWithTotp($username: String!, $code: String!) {
    loginWithTotp(username: $username, code: $code) {
      jwt
      refreshToken
      playerId
      displayName
      isGuest
    }
  }
`

const CONFIRM_TOTP = `
  mutation ConfirmTotp($code: String!) {
    confirmTotp(code: $code) {
      jwt
      playerId
      displayName
      refreshToken
      isGuest
    }
  }
`

export function useLoginWithTotp() {
  return useMutation({
    mutationFn: (input: { username: string; code: string }) =>
      gqlNoJwt<TotpLoginResponse>(LOGIN_WITH_TOTP, input).then(
        (d) => d.loginWithTotp
      ),
  })
}

export function useConfirmTotp() {
  return useMutation({
    mutationFn: (input: { code: string }) =>
      gqlNoJwt<ConfirmTotpResponse>(CONFIRM_TOTP, input).then(
        (d) => d.confirmTotp
      ),
  })
}

export function useLogout() {
  return useMutation({
    mutationFn: async () => {
      const refreshToken = getLocalStorage("refreshToken")
      if (!refreshToken) return
      await gqlNoJwt<{ logout: boolean }>(LOGOUT_MUTATION, { refreshToken })
    },
  })
}
