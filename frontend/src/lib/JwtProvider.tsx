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
  isGuest,
} from "@/lib/local-storage.helpers.ts"

const JwtContext = createContext<JwtContextType | undefined>(undefined)

type JwtContextType = {
  jwt: string | null
  isPending: boolean
}

export function JwtProvider({ children }: { children: ReactNode }) {
  const [jwt, setJwt] = useState<string | null>(() => getLocalStorage("jwt"))
  const [isUserAGuest] = useState<boolean>(() => isGuest())

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
    enabled: isUserAGuest,
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

  return (
    <JwtContext.Provider
      value={{ jwt, isPending: !jwt && queryResult.isPending }}
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
