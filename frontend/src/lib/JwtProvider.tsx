import { useQuery } from "@tanstack/react-query"
import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react"
import type { GuestAuthResponse } from "./playerServiceTypes/AuthResponses"
import { gqlNoJwt } from "./graphql"
import {
  type AuthIdentity,
  clearAuthStorage,
  decodeJwtExp,
  ensureFreshJwt,
  getIsGuest,
  isJwtExpiringSoon,
  persistAuth,
} from "./auth"
import { getLocalStorage } from "./local-storage.helpers"
import { useLogout } from "@/hooks/useAuthMutations"

const JwtContext = createContext<JwtContextType | undefined>(undefined)

type JwtContextType = {
  jwt: string | null
  isGuest: boolean
  isPending: boolean
  setAuth: (identity: AuthIdentity) => void
  signOut: () => Promise<void>
}

export function JwtProvider({ children }: { children: ReactNode }) {
  const [jwt, setJwt] = useState<string | null>(() => getLocalStorage("jwt"))
  const [isGuest, setIsGuest] = useState<boolean>(() => getIsGuest())

  const guestQuery = useQuery({
    queryKey: ["loginAsGuest"],
    queryFn: () =>
      gqlNoJwt<GuestAuthResponse>(`
        mutation {
          loginAsGuest {
            jwt
            playerId
            displayName
          }
        }
      `),
    enabled: isGuest && !jwt,
  })

  const logoutMutation = useLogout()

  useEffect(() => {
    if (!guestQuery.isSuccess || jwt) return
    const { jwt: newJwt, displayName, playerId } = guestQuery.data.loginAsGuest
    persistAuth({
      jwt: newJwt,
      playerId,
      displayName,
      isGuest: true,
    })
    setJwt(newJwt)
    setIsGuest(true)
  }, [guestQuery.isSuccess])

  // proactive of jwt 60s before expiry
  useEffect(() => {
    if (!jwt || isGuest) return
    const exp = decodeJwtExp(jwt)
    if (exp === null) return
    const delay = Math.max(0, exp * 1000 - Date.now() - 60_000)
    const timer = setTimeout(async () => {
      await ensureFreshJwt()
      setJwt(getLocalStorage("jwt"))
      setIsGuest(getIsGuest())
    }, delay)
    return () => clearTimeout(timer)
  }, [jwt, isGuest])

  // refresh on tab return because timers are throttled when off tab
  useEffect(() => {
    if (!jwt || isGuest) return
    const handler = async () => {
      if (document.visibilityState !== "visible") return
      const stored = getLocalStorage("jwt")
      if (!stored || !isJwtExpiringSoon(stored)) return
      await ensureFreshJwt()
      setJwt(getLocalStorage("jwt"))
      setIsGuest(getIsGuest())
    }
    document.addEventListener("visibilitychange", handler)
    return () => document.removeEventListener("visibilitychange", handler)
  }, [jwt, isGuest])

  const setAuth = (identity: AuthIdentity) => {
    persistAuth(identity)
    setJwt(identity.jwt)
    setIsGuest(identity.isGuest)
  }

  const signOut = async () => {
    try {
      await logoutMutation.mutateAsync()
    } catch {
      // backend logout is best-effort; clear local state regardless
    }
    clearAuthStorage()
    setJwt(null)
    setIsGuest(true)
  }

  return (
    <JwtContext.Provider
      value={{
        jwt,
        isGuest,
        isPending: !jwt && guestQuery.isPending,
        setAuth,
        signOut,
      }}
    >
      {children}
    </JwtContext.Provider>
  )
}

export function useJwt() {
  const context = useContext(JwtContext)
  if (!context) throw new Error("useJwt must be used within JwtProvider")
  return context
}
