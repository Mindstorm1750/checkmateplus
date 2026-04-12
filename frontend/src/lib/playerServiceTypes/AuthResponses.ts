type AuthResponse = {
    jwt: string,
    refreshToken?: string,
    playerId: string,
    displayName: string,
    isGuest: Boolean
}

export type GuestAuthResponse = { loginAsGuest: Pick<AuthResponse, "jwt" | "playerId" | "displayName"> }

export type RegisteredAuthResponse = { loginAsGuest: Required<AuthResponse> }