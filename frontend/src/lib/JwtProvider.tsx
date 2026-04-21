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
  getLocalStorage,
  setLocalStorage,
} from "@/lib/local-storage.helpers.ts"

const JwtContext = createContext<JwtContextType | undefined>(undefined)

type AuthedIdentity = {
  jwt: string
  playerId: string
  displayName: string
}

type JwtContextType = {
  jwt: string | null
  isGuest: boolean
  isPending: boolean
  setAuth: (identity: AuthedIdentity) => void
  signOut: () => void
}

export function JwtProvider({ children }: { children: ReactNode }) {
  const [jwt, setJwt] = useState<string | null>(() => getLocalStorage("jwt"))
  const [isGuest, setIsGuest] = useState<boolean>(
    JSON.parse(getLocalStorage("isGuest") ?? "true") as boolean
  )

  const queryResult = useQuery({
    queryKey: ["loginAsGuest"],
    queryFn: () =>
      gqlNoJwt<GuestAuthResponse>(`
                        mutation {
                            loginAsGuest {
                                jwt,
                                playerId,
                                displayName
                            }
                        }
                        `),
    enabled: isGuest,
  })

  useEffect(() => {
    if (queryResult.isSuccess) {
      const {
        jwt: newJwt,
        displayName,
        playerId,
      } = queryResult.data.loginAsGuest
      setLocalStorage("jwt", newJwt)
      setLocalStorage("displayName", displayName)
      setLocalStorage("playerId", playerId)
      setJwt(newJwt)
    }
  }, [queryResult.isSuccess])

  const setAuth = ({ jwt: newJwt, playerId, displayName }: AuthedIdentity) => {
    setLocalStorage("jwt", newJwt)
    setLocalStorage("playerId", playerId)
    setLocalStorage("displayName", displayName)
    setLocalStorage("isGuest", "false")
    setJwt(newJwt)
    setIsGuest(false)
  }

  const signOut = () => {
    setLocalStorage("isGuest", "true")
    setJwt(null)
    setIsGuest(true)
  }

  return (
    <JwtContext.Provider
      value={{
        jwt,
        isGuest,
        isPending: !jwt && queryResult.isPending,
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
