import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent.ts"
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest.ts"

export type DrawOfferState = "noOffer" | "waitingForResponse" | "receivedOffer"

/**
 * The joinRequest is used for making a "re-queue" button after a game ends
 */
export type MatchRedirectType = {
  matchFoundEvent: MatchFoundEvent
  joinRequest: JoinRequest
}
