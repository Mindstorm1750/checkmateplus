import type { TimeControl } from "../stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"

export type PlayerStats = {
    playerId: string,
    displayName: string,
    skillRating?: number,
    ratingDeviation?: number,
    wins: number
    losses: number
    draws: number
    totalGames: number
    winRate: number
    winsAsWhite: number
    winsAsBlack: number
    gamesAsWhite: number
    gamesAsBlack: number
    currentStreak: number
    longestWinStreak: number
    averageGameDurationMs: number,
    favouriteTimeControl?: TimeControl,
    memberSinceEpochMs: number
}