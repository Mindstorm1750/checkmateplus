const PLAYER_SERVICE_URL = import.meta.env.VITE_PLAYER_SERVICE_URL + '/graphql'

export async function gql<T>(
    query: string,
    variables?: Record<string, unknown>
): Promise<T> {
    const token = localStorage.getItem('jwt')
    const res = await fetch(PLAYER_SERVICE_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ query, variables }),
    })
    const { data, errors } = await res.json()
    if (errors?.length) throw new Error(errors[0].message)
    return data as T
}

