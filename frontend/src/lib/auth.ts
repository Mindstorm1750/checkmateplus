import { gqlNoJwt } from "./graphql"
import {
  getLocalStorage,
  removeLocalStorage,
  setLocalStorage,
} from "./local-storage.helpers"

export type AuthIdentity = {
  jwt: string
  refreshToken?: string | null
  playerId: string
  displayName: string
  isGuest: boolean
}

export const REFRESH_MUTATION = `
  mutation Refresh($refreshToken: String!) {
    refresh(refreshToken: $refreshToken) {
      jwt
      refreshToken
      playerId
      displayName
      isGuest
    }
  }
`

export const LOGOUT_MUTATION = `
  mutation Logout($refreshToken: String!) {
    logout(refreshToken: $refreshToken)
  }
`

const REFRESH_SKEW_SECONDS = 60

export function getIsGuest(): boolean {
  const raw = getLocalStorage("isGuest")
  if (raw === null) return true
  return raw === "true"
}

export function persistAuth(identity: AuthIdentity): void {
  setLocalStorage("jwt", identity.jwt)
  setLocalStorage("playerId", identity.playerId)
  setLocalStorage("displayName", identity.displayName)
  setLocalStorage("isGuest", identity.isGuest ? "true" : "false")
  if (!identity.isGuest && identity.refreshToken) {
    setLocalStorage("refreshToken", identity.refreshToken)
  } else {
    removeLocalStorage("refreshToken")
  }
}

export function clearAuthStorage(): void {
  removeLocalStorage("jwt")
  removeLocalStorage("refreshToken")
  removeLocalStorage("playerId")
  removeLocalStorage("displayName")
  setLocalStorage("isGuest", "true")
}

export function decodeJwtExp(token: string): number | null {
  const parts = token.split(".")
  if (parts.length !== 3) return null
  try {
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/")
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4)
    const json = JSON.parse(atob(padded)) as { exp?: unknown }
    return typeof json.exp === "number" ? json.exp : null
  } catch {
    return null
  }
}

export function isJwtExpiringSoon(
  token: string,
  skewSeconds = REFRESH_SKEW_SECONDS
): boolean {
  const exp = decodeJwtExp(token)
  if (exp === null) return true
  return exp - Math.floor(Date.now() / 1000) <= skewSeconds
}

let inFlightRefresh: Promise<string | null> | null = null

export async function ensureFreshJwt(): Promise<string | null> {
  if (inFlightRefresh) return inFlightRefresh

  const jwt = getLocalStorage("jwt")
  if (!jwt) return null
  if (getIsGuest()) return jwt
  if (!isJwtExpiringSoon(jwt)) return jwt

  const refreshToken = getLocalStorage("refreshToken")
  if (!refreshToken) return jwt

  inFlightRefresh = (async () => {
    try {
      const data = await gqlNoJwt<{ refresh: AuthIdentity }>(REFRESH_MUTATION, {
        refreshToken,
      })
      persistAuth(data.refresh)
      return data.refresh.jwt
    } catch {
      clearAuthStorage()
      return null
    } finally {
      inFlightRefresh = null
    }
  })()
  return inFlightRefresh
}
