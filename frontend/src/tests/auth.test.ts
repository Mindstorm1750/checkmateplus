import { afterEach, beforeEach, describe, expect, test, vi } from "vitest"

{
  const store = new Map<string, string>()
  const stub: Storage = {
    get length() {
      return store.size
    },
    clear: () => store.clear(),
    getItem: (k) => (store.has(k) ? store.get(k)! : null),
    key: (i) => Array.from(store.keys())[i] ?? null,
    removeItem: (k) => {
      store.delete(k)
    },
    setItem: (k, v) => {
      store.set(k, String(v))
    },
  }
  Object.defineProperty(globalThis, "localStorage", {
    value: stub,
    writable: true,
    configurable: true,
  })
}

function makeJwt(expSeconds: number | null): string {
  const header = btoa(JSON.stringify({ alg: "HS256", typ: "JWT" }))
  const payloadObj: Record<string, unknown> = { sub: "p1" }
  if (expSeconds !== null) payloadObj.exp = expSeconds
  const payload = btoa(JSON.stringify(payloadObj))
  return `${header}.${payload}.sig`
}

beforeEach(() => {
  localStorage.clear()
  vi.resetModules()
  vi.useRealTimers()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe("decodeJwtExp", () => {
  test("returns exp for valid token", async () => {
    const { decodeJwtExp } = await import("@/lib/auth")
    expect(decodeJwtExp(makeJwt(1234567890))).toBe(1234567890)
  })

  test("returns null for malformed token", async () => {
    const { decodeJwtExp } = await import("@/lib/auth")
    expect(decodeJwtExp("not.a.jwt")).toBeNull()
    expect(decodeJwtExp("only-one-part")).toBeNull()
  })

  test("returns null when exp claim missing", async () => {
    const { decodeJwtExp } = await import("@/lib/auth")
    expect(decodeJwtExp(makeJwt(null))).toBeNull()
  })
})

describe("isJwtExpiringSoon", () => {
  test("past exp counts as expiring", async () => {
    const { isJwtExpiringSoon } = await import("@/lib/auth")
    const past = Math.floor(Date.now() / 1000) - 10
    expect(isJwtExpiringSoon(makeJwt(past), 60)).toBe(true)
  })

  test("within skew counts as expiring", async () => {
    const { isJwtExpiringSoon } = await import("@/lib/auth")
    const soon = Math.floor(Date.now() / 1000) + 30
    expect(isJwtExpiringSoon(makeJwt(soon), 60)).toBe(true)
  })

  test("far future does not count", async () => {
    const { isJwtExpiringSoon } = await import("@/lib/auth")
    const far = Math.floor(Date.now() / 1000) + 3600
    expect(isJwtExpiringSoon(makeJwt(far), 60)).toBe(false)
  })
})

describe("persistAuth", () => {
  test("writes refresh token for non-guest", async () => {
    const { persistAuth } = await import("@/lib/auth")
    const futureExp = Math.floor(Date.now() / 1000) + 3600
    persistAuth({
      jwt: makeJwt(futureExp),
      refreshToken: "rt-1",
      playerId: "p1",
      displayName: "Alice",
      isGuest: false,
    })
    expect(localStorage.getItem("jwt")).toBe(makeJwt(futureExp))
    expect(localStorage.getItem("refreshToken")).toBe("rt-1")
    expect(localStorage.getItem("isGuest")).toBe("false")
    expect(localStorage.getItem("playerId")).toBe("p1")
    expect(localStorage.getItem("displayName")).toBe("Alice")
  })

  test("skips refresh token for guest", async () => {
    const { persistAuth } = await import("@/lib/auth")
    localStorage.setItem("refreshToken", "stale")
    const futureExp = Math.floor(Date.now() / 1000) + 3600
    persistAuth({
      jwt: makeJwt(futureExp),
      refreshToken: null,
      playerId: "g1",
      displayName: "Guest",
      isGuest: true,
    })
    expect(localStorage.getItem("refreshToken")).toBeNull()
    expect(localStorage.getItem("isGuest")).toBe("true")
  })
})

describe("ensureFreshJwt", () => {
  test("returns existing jwt when not expiring", async () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal("fetch", fetchSpy)
    const farFuture = Math.floor(Date.now() / 1000) + 3600
    const jwt = makeJwt(farFuture)
    localStorage.setItem("jwt", jwt)
    localStorage.setItem("refreshToken", "rt-old")
    localStorage.setItem("isGuest", "false")
    const { ensureFreshJwt } = await import("@/lib/auth")
    expect(await ensureFreshJwt()).toBe(jwt)
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  test("returns null when no jwt", async () => {
    const { ensureFreshJwt } = await import("@/lib/auth")
    expect(await ensureFreshJwt()).toBeNull()
  })

  test("guest path skips refresh", async () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal("fetch", fetchSpy)
    const soon = Math.floor(Date.now() / 1000) + 5
    const jwt = makeJwt(soon)
    localStorage.setItem("jwt", jwt)
    localStorage.setItem("isGuest", "true")
    const { ensureFreshJwt } = await import("@/lib/auth")
    expect(await ensureFreshJwt()).toBe(jwt)
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  test("refreshes when expiring and persists new tokens", async () => {
    const farFuture = Math.floor(Date.now() / 1000) + 3600
    const newJwt = makeJwt(farFuture)
    const fetchSpy = vi.fn(async () =>
      new Response(
        JSON.stringify({
          data: {
            refresh: {
              jwt: newJwt,
              refreshToken: "rt-new",
              playerId: "p1",
              displayName: "Alice",
              isGuest: false,
            },
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      )
    )
    vi.stubGlobal("fetch", fetchSpy)
    const soon = Math.floor(Date.now() / 1000) + 5
    localStorage.setItem("jwt", makeJwt(soon))
    localStorage.setItem("refreshToken", "rt-old")
    localStorage.setItem("isGuest", "false")
    const { ensureFreshJwt } = await import("@/lib/auth")
    expect(await ensureFreshJwt()).toBe(newJwt)
    expect(fetchSpy).toHaveBeenCalledOnce()
    expect(localStorage.getItem("jwt")).toBe(newJwt)
    expect(localStorage.getItem("refreshToken")).toBe("rt-new")
  })

  test("two concurrent callers share one fetch", async () => {
    const farFuture = Math.floor(Date.now() / 1000) + 3600
    const newJwt = makeJwt(farFuture)
    const fetchSpy = vi.fn(async () =>
      new Response(
        JSON.stringify({
          data: {
            refresh: {
              jwt: newJwt,
              refreshToken: "rt-new",
              playerId: "p1",
              displayName: "Alice",
              isGuest: false,
            },
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      )
    )
    vi.stubGlobal("fetch", fetchSpy)
    const soon = Math.floor(Date.now() / 1000) + 5
    localStorage.setItem("jwt", makeJwt(soon))
    localStorage.setItem("refreshToken", "rt-old")
    localStorage.setItem("isGuest", "false")
    const { ensureFreshJwt } = await import("@/lib/auth")
    const [a, b] = await Promise.all([ensureFreshJwt(), ensureFreshJwt()])
    expect(a).toBe(newJwt)
    expect(b).toBe(newJwt)
    expect(fetchSpy).toHaveBeenCalledOnce()
  })

  test("clears storage and returns null on refresh failure", async () => {
    const fetchSpy = vi.fn(async () =>
      new Response(
        JSON.stringify({ errors: [{ message: "invalid refresh token" }] }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      )
    )
    vi.stubGlobal("fetch", fetchSpy)
    const soon = Math.floor(Date.now() / 1000) + 5
    localStorage.setItem("jwt", makeJwt(soon))
    localStorage.setItem("refreshToken", "rt-bad")
    localStorage.setItem("isGuest", "false")
    const { ensureFreshJwt } = await import("@/lib/auth")
    expect(await ensureFreshJwt()).toBeNull()
    expect(localStorage.getItem("jwt")).toBeNull()
    expect(localStorage.getItem("refreshToken")).toBeNull()
    expect(localStorage.getItem("isGuest")).toBe("true")
  })
})
