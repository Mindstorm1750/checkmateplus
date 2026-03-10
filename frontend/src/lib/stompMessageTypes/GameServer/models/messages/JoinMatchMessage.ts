import type {PlayerPreferences} from "@/lib/stompMessageTypes/GameServer/models/messages/PlayerPreferences.ts";

export type JoinMatchMessage = {
    matchId: String,
    playerId: String,
    playerName: String,
    playerPreferences: PlayerPreferences
}