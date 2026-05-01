import Layout from "@/components/Layout"
import { BackArrowLink } from "@/components/ui/back-arrow-link"
import ChessBoard, {
  type ChessBoardProps,
  type PlayerLabelsType,
} from "@/components/ui/ChessBoard"
import {
  algebraicToSquare,
  fenToBoard,
  squareToAlgebraic,
} from "@/lib/chess.helpers"
import { usePreferences } from "@/lib/PreferencesProvider"
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage"
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage"
import type {
  MatchStateMessage,
  Player,
} from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import type { MoveMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MoveMessage"
import type { TimeUpdateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/TimeUpdateMessage"
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent"
import {
    EMPTY_BOARD,
    EMPTY_PIECE_ORIGINS,
    type Board,
    type Move,
    type Square,
    type PieceType, type VoiceMove,
} from "@/types/Chess.type"
import { Client, type IMessage } from "@stomp/stompjs"
import { useEffect, useRef, useState } from "react"
import { useLocation, useNavigate } from "react-router"
import SockJS from "sockjs-client/dist/sockjs"
import { useJwt } from "@/lib/JwtProvider"
import { ensureFreshJwt } from "@/lib/auth"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
} from "@/components/ui/dialog.tsx"
import { Button } from "@/components/ui/button.tsx"
import type { DrawOfferMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/DrawOfferMessage.ts"
import type { DrawOfferState } from "@/types/Match.type.ts"
import type { DrawResponseMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/DrawResponseMessage.ts"
import type { ResignMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/ResignMessage.ts"
import { usePlayerRating } from "@/hooks/usePlayerStats.ts"
import { VoiceMoveButton } from "@/components/ui/VoiceMoveButton.tsx"

function Match() {
  const navigate = useNavigate()
  /**
   * using browser History state property to store the match information.
   * Might be a temporary solution OR copy this data to a more persistent form like localStorage.
   */
  const { state }: { state: MatchFoundEvent } = useLocation()
  useEffect(() => {
    if (!state) {
      // redirect to main page if we've visited this page directly instead of going through matchmaking
      navigate("/")
    }
  }, [])
  if (!state) {
    return null // don't continue rendering if we haven't been redirected to this match page from matchmaking
  }
  const { matchId, websocketUrl, playerId, matchType } = state
  const [boardState, setBoardState] = useState<Board>(EMPTY_BOARD)
  const [lastMove, setLastMove] = useState<Move>()
  const [isCheck, setIsCheck] = useState<boolean>(false)
  /**
   * pieceOrigins is used in ChessBoard to animate pieces via framer motion's layoutId.
   * Identifies pieces as they move from boardState to boardState.
   */
  const [pieceOrigins, setPieceOrigins] =
    useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS)
  const [clockState, setClockState] = useState<ChessBoardProps["clockState"]>()
  const [isPlayersTurn, setIsPlayersTurn] = useState<boolean>(false)
  const [isPlayerWhite, setIsPlayerWhite] = useState<boolean>(true)
  const [isGameOver, setIsGameOver] = useState<boolean>(false)
  const [gameOverMessage, setGameOverMessage] = useState<String>()
  const [resignConfirmationDialog, setResignConfirmationDialog] =
    useState<boolean>(false)
  const [drawOfferState, setDrawOfferState] =
    useState<DrawOfferState>("noOffer")
  const { preferences } = usePreferences()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isPending: jwtIsPending } = useJwt()
  const [players, setPlayers] = useState<{
    whitePlayer: Player
    blackPlayer: Player
  }>()
  const { data: whitePlayerRatingData } = usePlayerRating(
    players?.whitePlayer.playerId ?? null
  )
  const { data: blackPlayerRatingData } = usePlayerRating(
    players?.blackPlayer.playerId ?? null
  )

  const playerLabels: PlayerLabelsType | undefined = players
    ? {
        whitePlayerLabel:
          players.whitePlayer.displayName +
          (matchType === "COMPETITIVE_PVP" && whitePlayerRatingData
            ? ` (${whitePlayerRatingData.skillRating})`
            : ""),
        blackPlayerLabel:
          players.blackPlayer.displayName +
          (matchType === "COMPETITIVE_PVP" && blackPlayerRatingData
            ? ` (${blackPlayerRatingData.skillRating})`
            : ""),
      }
    : undefined

  useEffect(() => {
    if (!jwtIsPending && jwt) {
      connect()
    }
  }, [matchId, jwtIsPending])

  const connect = () => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`https://${websocketUrl}/match`),
      beforeConnect: async () => {
        const fresh = (await ensureFreshJwt()) ?? jwt
        client.connectHeaders = { Authorization: `Bearer ${fresh}` }
      },
      debug: (str: string) => console.log(str),
      onConnect: () => {
        // Subscribe to your specific user queue for status updates
        client.subscribe(
          `/topic/match/${matchId}/events`,
          (message: IMessage) => {
            const body: MatchEventMessage = JSON.parse(message.body)
            console.log("Got match event message: " + body.message)
            if (body.eventType == "MATCH_ENDED") {
              // if the game has ended in checkmate, stalemate, or is a draw
              setIsGameOver(true)
              setGameOverMessage(body.message)
            }
            if (body.eventType == "TIME_EXPIRED") {
              // if the game has ended due to time
              setIsGameOver(true)
              setGameOverMessage(body.message)
            }
            if (body.eventType == "PLAYER_RESIGNED") {
              setIsGameOver(true)
              setGameOverMessage(body.message)
            }
            if (body.eventType == "DRAW_ACCEPTED") {
              // both players agree to draw
              setIsGameOver(true)
              setGameOverMessage(body.message)
            }
            if (body.eventType == "DRAW_DECLINED") {
              setDrawOfferState("noOffer")
            }
          }
        )

        client.subscribe(
          `/topic/match/${matchId}/time`,
          (message: IMessage) => {
            const timeUpdate: TimeUpdateMessage = JSON.parse(message.body)
            const { whitePlayerTime, blackPlayerTime } = timeUpdate
            setClockState({ whitePlayerTime, blackPlayerTime })
          }
        )

        client.subscribe(
          `/user/queue/match/${matchId}/state`,
          (message: IMessage) => {
            const match: MatchStateMessage = JSON.parse(message.body)
            console.log(`Got initial match state message`)
            setBoardState(fenToBoard(match.fen)) // should be the starting position
            if (match.whitePlayer && match.blackPlayer) {
              setPlayers({
                whitePlayer: match.whitePlayer,
                blackPlayer: match.blackPlayer,
              })
            }
            setIsPlayerWhite(match.whitePlayer?.playerId === playerId)
          }
        )

        // Subscribe to the match state. This will update our board
        client.subscribe(
          `/topic/match/${matchId}/state`,
          (message: IMessage) => {
            const match: MatchStateMessage = JSON.parse(message.body)
            console.log(
              `Is checkmate? ${match.isCheckmate}. Is stalemate? ${match.isStalemate}. Is draw? ${match.isDraw}`
            )
            const newBoard = fenToBoard(match.fen)
            const playerWithTurn =
              newBoard.sideToMove === "WHITE"
                ? match.whitePlayer
                : match.blackPlayer
            if (playerWithTurn?.playerId === playerId) {
              setIsPlayersTurn(true)
            }
            setBoardState(newBoard)
            setIsCheck(match.isCheck)
            setLastMove(
              match.lastMove && {
                from: algebraicToSquare(match.lastMove.from),
                to: algebraicToSquare(match.lastMove.to),
                promotion: match.lastMove.promotion,
              }
            )
            if (match.lastMove) {
              const move = {
                from: algebraicToSquare(match.lastMove.from),
                to: algebraicToSquare(match.lastMove.to),
                promotion: match.lastMove.promotion,
              }
              setPieceOrigins((prev) => {
                const originOfSquareMovedFrom = prev.get(move.from)
                if (
                  originOfSquareMovedFrom === undefined ||
                  originOfSquareMovedFrom === null
                ) {
                  // if pieceOrigins has already been updated with a move, the square we're moving from will have no origin (be undefined)
                  // therefore we should leave pieceOrigins as is.
                  return prev
                }
                const newPieceOrigins = new Map(prev)
                const origin = prev.get(move.from) ?? move.from
                const whiteCastledKingside = origin == 60 && move.to == 62
                const whiteCastledQueenside = origin == 60 && move.to == 58
                const blackCastledKingside = origin == 4 && move.to == 6
                const blackCastledQueenside = origin == 4 && move.to == 2
                if (whiteCastledKingside) {
                  newPieceOrigins.set(61, 63)
                  newPieceOrigins.delete(63)
                }
                if (whiteCastledQueenside) {
                  newPieceOrigins.set(59, 56)
                  newPieceOrigins.delete(56)
                }
                if (blackCastledKingside) {
                  newPieceOrigins.set(5, 7)
                  newPieceOrigins.delete(7)
                }
                if (blackCastledQueenside) {
                  newPieceOrigins.set(3, 0)
                  newPieceOrigins.delete(0)
                }
                newPieceOrigins.set(move.to, origin)
                newPieceOrigins.delete(move.from)
                return newPieceOrigins
              })
            }
          }
        )

        client.subscribe(
          `/topic/match/${matchId}/draw-offer`,
          (message: IMessage) => {
            const drawOfferMessage: DrawOfferMessage = JSON.parse(message.body)
            console.log(
              "Got draw offer message from: " + drawOfferMessage.offeredBy
            )
            if (drawOfferMessage.offeredBy != playerId) {
              setDrawOfferState("receivedOffer")
            }
          }
        )

        joinGame()
      },
    })

    client.activate()
    stompClient.current = client
  }

  const joinGame = () => {
    if (stompClient.current?.connected) {
      const joinRequest: JoinMatchMessage = {
        playerId,
        matchId,
        playerName: getLocalStorage("displayName") ?? "",
        playerPreferences: preferences,
      }

      stompClient.current.publish({
        destination: `/app/match/${matchId}/join`,
        body: JSON.stringify(joinRequest),
      })
    }
  }

  const handleMove: (move: Move) => void = (move) => {
    if (stompClient.current?.connected) {
      setIsPlayersTurn(false)
      const moveRequest: MoveMessage = {
        matchId,
        playerId,
        from: squareToAlgebraic(move.from),
        to: squareToAlgebraic(move.to),
        promotion: move.promotion,
      }

      stompClient.current.publish({
        destination: `/app/match/${matchId}/move`,
        body: JSON.stringify(moveRequest),
      })
    }
  }

  const handleResign = () => {
    const resignMessage: ResignMessage = {
      matchId,
      playerId,
    }
    if (stompClient.current?.connected) {
      stompClient.current.publish({
        destination: `/app/match/${matchId}/resign`,
        body: JSON.stringify(resignMessage),
      })
    }
  }

  const offerDraw = () => {
    if (stompClient.current?.connected) {
      setDrawOfferState("waitingForResponse")
      const drawOfferMessage: DrawOfferMessage = {
        matchId,
        offeredBy: playerId,
      }
      stompClient.current.publish({
        destination: `/app/match/${matchId}/draw/offer`,
        body: JSON.stringify(drawOfferMessage),
      })
    }
  }

  const respondToDraw = (accepted: boolean) => {
    if (stompClient.current?.connected) {
      setDrawOfferState("noOffer")
      const drawResponseMessage: DrawResponseMessage = {
        matchId,
        playerId,
        accepted,
      }
      stompClient.current.publish({
        destination: `/app/match/${matchId}/draw/respond`,
        body: JSON.stringify(drawResponseMessage),
      })
    }
  }

  const handleVoiceMove = (voiceMove: VoiceMove) => {
    if (!voiceMove.from || !voiceMove.to) {
      console.warn("Voice move missing resolved squares", voiceMove)
      return
    }

    const from = algebraicToSquare(voiceMove.from)
    const to = algebraicToSquare(voiceMove.to)

    if (from == null || to == null) {
      console.warn("Voice move contained invalid algebraic squares", voiceMove)
      return
    }

    handleMove({
      from,
      to,
      promotion: voiceMove.promotion as PieceType ?? undefined,
    })
  }

  return (
    <Layout>
      <Dialog open={resignConfirmationDialog}>
        <div className="flex w-full justify-center">
          <div className="w-full md:w-3/4">
            {isGameOver && gameOverMessage && (
              <p className="mb-3 rounded-md p-3 outline-2 outline-solid">
                {gameOverMessage}
              </p>
            )}
            <ChessBoard
              isPlayersTurn={isPlayersTurn}
              isGameOver={isGameOver}
              isCheck={isCheck}
              lastMove={lastMove}
              boardState={boardState}
              clockState={clockState}
              moveCallback={handleMove}
              whitePerspective={isPlayerWhite}
              pieceOrigins={pieceOrigins}
              playerLabels={playerLabels}
            >
              {!isGameOver && (
                <div className="flex items-center gap-2">
                  <Button
                    onClick={() => setResignConfirmationDialog(true)}
                    variant="outline"
                  >
                    Resign
                  </Button>
                  {drawOfferState == "noOffer" && (
                    <Button onClick={offerDraw} variant="outline">
                      Offer draw
                    </Button>
                  )}
                  {drawOfferState == "waitingForResponse" && (
                    <Button disabled variant="outline">
                      Draw offered
                    </Button>
                  )}
                  {drawOfferState == "receivedOffer" && (
                    <>
                      <Button onClick={() => respondToDraw(true)}>
                        Accept draw
                      </Button>
                      <Button onClick={() => respondToDraw(false)}>
                        Reject draw
                      </Button>
                    </>
                  )}
                </div>
              )}
            </ChessBoard>
            <div className="flex w-full justify-center">
              <VoiceMoveButton
                matchId={matchId}
                moveCallback={handleVoiceMove}
                boardState={boardState}
                whitePerspective={isPlayerWhite}
                isInteractable={isPlayersTurn && !isGameOver}
              />
            </div>
          </div>
        </div>
        {isGameOver && <BackArrowLink />}
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogDescription>
              <p>Resign this game?</p>
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              onClick={() => {
                handleResign()
                setResignConfirmationDialog(false)
              }}
              variant="destructive"
            >
              Resign
            </Button>
            <DialogClose asChild>
              <Button
                onClick={() => setResignConfirmationDialog(false)}
                variant="outline"
              >
                Cancel
              </Button>
            </DialogClose>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Layout>
  )
}

export default Match
