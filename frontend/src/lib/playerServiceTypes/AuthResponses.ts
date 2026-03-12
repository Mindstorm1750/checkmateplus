type AuthResponse = {
    jwt: string,
    refreshToken?: string,
    playerId: string,
    displayName: string,
    isGuest: Boolean
}

export type GuestAuthResponse = Pick<AuthResponse, "jwt" | "playerId" | "displayName">

export type RegisteredAuthResponse = Required<AuthResponse>