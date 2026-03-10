/**
 * This type is what the backend uses.
 * String instead of specific values like "BROWN" or "FUN" to be more flexible in case we expand customisation options
 */
export type PlayerPreferences = {
    boardColour: String,
    animationType: String,
}
