/**
 * Result of a game server allocation request.
 *
 * @property success Whether the allocation was successful.
 * @property serverId The ID of the allocated server.
 * @property matchId The ID of the match allocated to the server.
 * @property websocketUrl The URL to connect to the server via WebSockets.
 * @property estimatedWaitSeconds Estimated wait time if the match hasn't started yet.
 * @property message Additional information or error message.
 */
export type ServerAllocationResult = {
  success: Boolean
  serverId: String
  matchId: String
  websocketUrl: String
  estimatedWaitSeconds: number
  message: String
}
