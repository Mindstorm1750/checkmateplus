import type { BotConfiguration } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/BotConfiguration";
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType";
import type { TimeControl } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl";

/**
 * Request payload for a player joining the matchmaking queue.
 *
 * @property displayName The name the player wish to display during the match.
 */
export type JoinRequest = { displayName: String, matchType: MatchType, timeControl?: TimeControl,  botConfiguration?: BotConfiguration }