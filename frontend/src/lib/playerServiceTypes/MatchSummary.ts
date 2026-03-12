import type { Colour } from "@/types/Chess.type"
import type { MatchType } from "../stompMessageTypes/ChessLibrary/PlayerModels/MatchType"
import type { TimeControl } from "../stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import type { MatchResult } from "./MatchResult"

export type MatchSummary = {
    matchId: string,
    opponentId?: string,
    result: MatchResult,
    colour: Colour,
    matchType: MatchType,
    timeControl: TimeControl,
    gameDurationMs?: number,
    createdAtEpochMs: number
}