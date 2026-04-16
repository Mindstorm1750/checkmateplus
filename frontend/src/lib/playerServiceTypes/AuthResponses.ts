type AuthResponse = {
  jwt: string
  refreshToken?: string
  playerId: string
  displayName: string
  isGuest: boolean
}

export type GuestAuthResponse = {
  loginAsGuest: Pick<AuthResponse, "jwt" | "playerId" | "displayName">
}

export type RegisteredAuthResponse = { loginAsGuest: Required<AuthResponse> }

export type TotpLoginResponse = {
  loginWithTotp: Pick<
    AuthResponse,
    "jwt" | "playerId" | "displayName" | "isGuest" | "refreshToken"
  >
}
