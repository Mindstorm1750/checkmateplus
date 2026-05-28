/**
 * All the localStorage keys we use
 */
export type LocalStorageKeys =
  | "jwt"
  | "refreshToken"
  | "playerId"
  | "displayName"
  | "isGuest"
  | "chessPreferences"
  | "vite-ui-theme"

/**
 * A wrapper around localStorage.setItem so that we only set keys that are of type LocalStorageKeys
 */
export const setLocalStorage = (key: LocalStorageKeys, value: string): void => {
  localStorage.setItem(key, value)
}

export const getLocalStorage = (key: LocalStorageKeys): string | null => {
  return localStorage.getItem(key)
}

export const removeLocalStorage = (key: LocalStorageKeys): void => {
  localStorage.removeItem(key)
}
