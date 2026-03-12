import { useQuery } from "@tanstack/react-query";
import { createContext, useContext, useEffect, useState, type Dispatch, type ReactNode, type SetStateAction } from "react";
import type { GuestAuthResponse } from "./playerServiceTypes/AuthResponses";
import { gql } from "./graphql";

const JwtContext = createContext<JwtContextType | undefined>(undefined);

type JwtContextType = {
    jwt: string,
    setJwt: Dispatch<SetStateAction<string>>
}

export function JwtProvider({ children }: { children: ReactNode }) {
    const [jwt, setJwt] = useState<string>(() => {
        // Load from localStorage. Default to brown board and normal animations if nothing stored.
        const saved = localStorage.getItem('jwt');
        if (!saved) {
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
            if (queryResult.data) {
                localStorage.setItem('jwt', queryResult.data.jwt)
                localStorage.setItem('playerId', queryResult.data.playerId)
                localStorage.setItem('displayName', queryResult.data.displayName)
            }
        }
        return saved ?? 'error'; // Realistically, the query should give us an actual jwt
    });

    // Save to localStorage whenever jwt changes.
    useEffect(() => {
        localStorage.setItem('jwt', jwt);
    }, [jwt]);

    return (
        <JwtContext.Provider value={{ jwt, setJwt }}>
            {children}
        </JwtContext.Provider>
    );
}

export function useJwt() {
    const context = useContext(JwtContext);
    if (!context) throw new Error('useJwt must be used within JwtProvider');
    return context;
}