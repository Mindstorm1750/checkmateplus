import Layout from "@/components/Layout"
import { BackArrowLink } from "@/components/ui/back-arrow-link"
import ChessBoard, { type ChessBoardProps } from "@/components/ui/ChessBoard"
import { Spinner } from "@/components/ui/spinner"
import { algebraicToSquare, fenToBoard } from "@/lib/chess.helpers"
import { useJwt } from "@/lib/JwtProvider"
import { ensureFreshJwt } from "@/lib/auth"
import type { SpectateMatch as SpectateMatchType } from "@/lib/playerServiceTypes/SpectateMatch"
import {
  GAME_OVER_TITLES,
  type MatchEventMessage,
  type MatchEventType,
} from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage"
import type {
  MatchStateMessage,
  MoveHistoryEntry,
} from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import type { TimeUpdateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/TimeUpdateMessage"
import { applyBackwardStep, applyForwardStep } from "@/lib/pieceOrigins.helpers"
import MatchAnalysisPanel from "@/components/match-analysis/MatchAnalysisPanel"
import {
  EMPTY_BOARD,
  EMPTY_PIECE_ORIGINS,
  type Board,
  type Square,
} from "@/types/Chess.type"
import { useSpectateMatch } from "@/hooks/useSpectateMatch"
import { Client, type IMessage } from "@stomp/stompjs"
import { useEffect, useRef, useState } from "react"
import { useLocation, useNavigate, useParams } from "react-router"
import SockJS from "sockjs-client/dist/sockjs"
import { usePlayerRating } from "@/hooks/usePlayerStats.ts"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog.tsx"
import { Button } from "@/components/ui/button.tsx"

function SpectateMatch() {
  const { matchId } = useParams<{ matchId: string }>()
  const navigate = useNavigate()
  const { state } = useLocation() as { state: SpectateMatchType | null }

  const haveRouterState = !!state?.websocketUrl
  const {
    data: queriedMatch,
    isPending: queryPending,
    isError: queryError,
  } = useSpectateMatch(matchId, !haveRouterState)

  const matchSummary: SpectateMatchType | null | undefined = haveRouterState
    ? state
    : queriedMatch
  const websocketUrl = matchSummary?.websocketUrl

  const [boardState, setBoardState] = useState<Board>(EMPTY_BOARD)
  const [isCheck, setIsCheck] = useState<boolean>(false)
  const [pieceOrigins, setPieceOrigins] =
    useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS)
  const [moveHistory, setMoveHistory] = useState<MoveHistoryEntry[]>([])
  const [previewIndex, setPreviewIndex] = useState<number | null>(null)
  const [previewPieceOrigins, setPreviewPieceOrigins] =
    useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS)
  const priorDisplayedIndexRef = useRef<number | null>(null)
  const [clockState, setClockState] = useState<ChessBoardProps["clockState"]>({
    whitePlayerTime: matchSummary?.whiteTimeSeconds ?? 0,
    blackPlayerTime: matchSummary?.blackTimeSeconds ?? 0,
  })
  const [isGameOver, setIsGameOver] = useState(false)
  const [gameOverMessage, setGameOverMessage] = useState<string>()
  const [gameOverDialogOpen, setGameOverDialogOpen] = useState(false)
  const [gameOverEventType, setGameOverEventType] = useState<MatchEventType>()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isPending: jwtIsPending } = useJwt()
  const { data: whitePlayerRatingData } = usePlayerRating(
    matchSummary?.whitePlayer?.playerId ?? null
  )
  const { data: blackPlayerRatingData } = usePlayerRating(
    matchSummary?.blackPlayer?.playerId ?? null
  )

  const playerLabels = matchSummary
    ? {
        whitePlayerLabel: `${matchSummary.whitePlayer?.displayName ?? ""}${whitePlayerRatingData?.skillRating ? ` (${whitePlayerRatingData.skillRating})` : ""}`,
        blackPlayerLabel: `${matchSummary.blackPlayer?.displayName ?? ""}${blackPlayerRatingData?.skillRating ? ` (${blackPlayerRatingData.skillRating})` : ""}`,
      }
    : undefined

  useEffect(() => {
    if (jwtIsPending || !jwt || !matchId || !websocketUrl) return
    if (stompClient.current) return

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(
          `${import.meta.env.VITE_HTTP_PROTOCOL}://${websocketUrl}/match`
        ),
      beforeConnect: async () => {
        const fresh = (await ensureFreshJwt()) ?? jwt
        client.connectHeaders = { Authorization: `Bearer ${fresh}` }
      },
      debug: () => {},
      onConnect: () => {
        client.subscribe(
          `/user/queue/match/${matchId}/state`,
          (message: IMessage) => {
            const match: MatchStateMessage = JSON.parse(message.body)
            setBoardState(fenToBoard(match.fen as string))
            setClockState({
              whitePlayerTime: match.whitePlayerTime,
              blackPlayerTime: match.blackPlayerTime,
            })
            setMoveHistory(match.moveHistory ?? [])
          }
        )

        client.subscribe(`/user/queue/errors`, (message: IMessage) => {
          const body = JSON.parse(message.body) as {
            error?: string
            matchId?: string
          }
          if (body.error === "Match not found") {
            navigate("/spectate", { replace: true })
          }
        })

        client.subscribe(
          `/topic/match/${matchId}/state`,
          (message: IMessage) => {
            const match: MatchStateMessage = JSON.parse(message.body)
            const newBoard = fenToBoard(match.fen as string)
            setIsCheck(match.isCheck)
            setBoardState(newBoard)
            if (match.lastMove) {
              const move = {
                from: algebraicToSquare(match.lastMove.from as string),
                to: algebraicToSquare(match.lastMove.to as string),
              }
              setPieceOrigins((prev) => applyForwardStep(prev, move))
            }
            setMoveHistory(match.moveHistory ?? [])
          }
        )

        client.subscribe(
          `/topic/match/${matchId}/time`,
          (message: IMessage) => {
            const t: TimeUpdateMessage = JSON.parse(message.body)
            setClockState({
              whitePlayerTime: t.whitePlayerTime,
              blackPlayerTime: t.blackPlayerTime,
            })
          }
        )

        client.subscribe(
          `/topic/match/${matchId}/events`,
          (message: IMessage) => {
            const body: MatchEventMessage = JSON.parse(message.body)
            if (
              body.eventType === "MATCH_ENDED" ||
              body.eventType === "TIME_EXPIRED" ||
              body.eventType === "PLAYER_RESIGNED" ||
              body.eventType === "DRAW_ACCEPTED"
            ) {
              setIsGameOver(true)
              setGameOverEventType(body.eventType)
              setGameOverDialogOpen(true)
              setGameOverMessage(body.message as string)
            }
          }
        )

        client.publish({
          destination: `/app/match/${matchId}/spectate`,
          body: "",
        })
      },
    })

    client.activate()
    stompClient.current = client

    return () => {
      client.deactivate()
      stompClient.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [matchId, websocketUrl, jwt, jwtIsPending])

  const isPreviewing = previewIndex !== null
  const displayedBoardState =
    isPreviewing && moveHistory[previewIndex!]
      ? fenToBoard(moveHistory[previewIndex!].fenAfter)
      : boardState
  const displayedPieceOrigins = isPreviewing
    ? previewPieceOrigins
    : pieceOrigins

  useEffect(() => {
    const liveIdx = moveHistory.length - 1
    const priorRaw = priorDisplayedIndexRef.current
    const priorIdx = priorRaw ?? liveIdx
    const nextIdx = previewIndex ?? liveIdx

    priorDisplayedIndexRef.current = previewIndex

    if (nextIdx === priorIdx) return

    const baseMap = priorRaw === null ? pieceOrigins : previewPieceOrigins

    if (nextIdx === priorIdx + 1 && moveHistory[nextIdx]) {
      const move = moveHistory[nextIdx]
      setPreviewPieceOrigins(
        applyForwardStep(baseMap, {
          from: algebraicToSquare(move.from),
          to: algebraicToSquare(move.to),
        })
      )
    } else if (nextIdx === priorIdx - 1 && moveHistory[priorIdx]) {
      const move = moveHistory[priorIdx]
      setPreviewPieceOrigins(
        applyBackwardStep(baseMap, {
          from: algebraicToSquare(move.from),
          to: algebraicToSquare(move.to),
        })
      )
    } else {
      setPreviewPieceOrigins(EMPTY_PIECE_ORIGINS)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [previewIndex])

  if (!matchId) {
    return (
      <Layout>
        <p className="text-destructive">Missing match id.</p>
        <BackArrowLink />
      </Layout>
    )
  }

  if (!haveRouterState && queryPending) {
    return (
      <Layout>
        <Spinner />
      </Layout>
    )
  }

  if (!haveRouterState && (queryError || !queriedMatch)) {
    return (
      <Layout>
        <p className="text-destructive">Match not found or no longer live.</p>
        <BackArrowLink />
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="flex w-full justify-center">
        <div className="flex w-full flex-col gap-4 md:w-11/12 md:flex-row lg:w-5/6">
          <div className="min-w-0 md:flex-1">
            <ChessBoard
              readOnly
              isPlayersTurn={false}
              isGameOver={isGameOver}
              boardState={displayedBoardState}
              clockState={clockState}
              moveCallback={() => {}}
              whitePerspective={true}
              pieceOrigins={displayedPieceOrigins}
              isCheck={isCheck}
              playerLabels={playerLabels}
            />
          </div>
          <div className="w-full shrink-0 md:w-72 lg:w-80">
            <MatchAnalysisPanel
              moves={moveHistory}
              selectedIndex={previewIndex}
              onSelect={setPreviewIndex}
            />
          </div>
        </div>
      </div>
      <Dialog
        open={gameOverDialogOpen}
        onOpenChange={(open) => !open && setGameOverDialogOpen(false)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {gameOverEventType && GAME_OVER_TITLES[gameOverEventType]}
            </DialogTitle>
            <DialogDescription>{gameOverMessage}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button onClick={() => setGameOverDialogOpen(false)}>Close</Button>
            <Button onClick={() => navigate("/spectate")}>
              Back to Spectate
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <BackArrowLink />
    </Layout>
  )
}

export default SpectateMatch
