import { useQuery } from "@tanstack/react-query";
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { GuestAuthResponse } from "./playerServiceTypes/AuthResponses";
import { gql } from "./graphql";

const JwtContext = createContext<JwtContextType | undefined>(undefined);

type JwtContextType = {
    jwt: string | null,
    isPending: boolean
}

export function JwtProvider({ children }: { children: ReactNode }) {
    const [jwt, setJwt] = useState<string | null>(() => localStorage.getItem('jwt'));

    const queryResult = useQuery({
        queryKey: ['loginAsGuest'],
        queryFn: () =>
            gql<GuestAuthResponse>(`
                        mutation {
                            loginAsGuest {
                                jwt,
                                playerId,
                                displayName
                            }
                        }
                        `),
    })

    useEffect(() => {
        if (queryResult.isSuccess && !jwt) {
            const { jwt: newJwt, displayName, playerId } = queryResult.data.loginAsGuest;
            localStorage.setItem('jwt', newJwt)
            localStorage.setItem('displayName', displayName)
            localStorage.setItem('playerId', playerId)
            setJwt(newJwt);
        }
    }, [queryResult.isSuccess])

    return (
        <JwtContext.Provider value={{ jwt, isPending: !jwt && queryResult.isPending }}>
            {children}
        </JwtContext.Provider>
    );
}

export function useJwt() {
    const context = useContext(JwtContext);
    if (!context) throw new Error('useJwt must be used within JwtProvider');
    return context;
}