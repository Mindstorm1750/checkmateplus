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
export type RegisterResponse = {
  register: {
    qrCodeUri: string
    secret: string
    playerId: string
  }
}

export type ConfirmTotpResponse = {
  confirmTotp: {
    jwt: string
    playerId: string
    displayName: string
    refreshToken?: string
    isGuest: boolean
  }
}
