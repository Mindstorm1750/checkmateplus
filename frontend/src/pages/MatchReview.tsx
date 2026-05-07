import Layout from "@/components/Layout"
import MatchAnalysisPanel from "@/components/match-analysis/MatchAnalysisPanel"
import type { MoveAnalysis } from "@/components/match-analysis"
import { BackArrowLink } from "@/components/ui/back-arrow-link"
import { Button } from "@/components/ui/button"
import ChessBoard from "@/components/ui/ChessBoard"
import { Spinner } from "@/components/ui/spinner"
import { useMatchAnalysis } from "@/hooks/useMatchAnalysis"
import { useMatchMoveHistory } from "@/hooks/useMatchMoveHistory"
import { useRequestMatchAnalysis } from "@/hooks/useRequestMatchAnalysis"
import { fenToBoard } from "@/lib/chess.helpers"
import { EMPTY_BOARD, EMPTY_PIECE_ORIGINS } from "@/types/Chess.type"
import { useMemo, useState } from "react"
import { useParams } from "react-router"

const outcomeLabel: Record<string, string> = {
  WHITE_WINS: "White wins",
  BLACK_WINS: "Black wins",
  DRAW: "Draw",
  ABANDONED: "Abandoned",
}

export default function MatchReview() {
  const { matchId } = useParams<{ matchId: string }>()
  const {
    data: history,
    isPending: historyPending,
    isError: historyError,
  } = useMatchMoveHistory(matchId)
  const { data: analysis } = useMatchAnalysis(matchId)
  const { mutate: requestAnalysis, isPending: requestPending } =
    useRequestMatchAnalysis()

  const [previewIndex, setPreviewIndex] = useState<number | null>(null)

  const analysisMap = useMemo(() => {
    const map = new Map<number, MoveAnalysis>()
    analysis?.moves.forEach((m) => map.set(m.ply, m))
    return map
  }, [analysis])

  if (!matchId) {
    return (
      <Layout>
        <p className="text-destructive">Missing match id.</p>
        <BackArrowLink />
      </Layout>
    )
  }

  if (historyPending) {
    return (
      <Layout>
        <Spinner />
      </Layout>
    )
  }

  if (historyError || !history) {
    return (
      <Layout>
        <p className="text-destructive">Match not found.</p>
        <BackArrowLink />
      </Layout>
    )
  }

  const moves = history.moves
  const isPreviewing = previewIndex !== null
  const displayedFen =
    isPreviewing && moves[previewIndex!]
      ? moves[previewIndex!].fenAfter
      : history.finalFen
  const displayedBoard = displayedFen ? fenToBoard(displayedFen) : EMPTY_BOARD

  const playerLabels = {
    whitePlayerLabel: history.whitePlayer
      ? `${history.whitePlayer.displayName}${history.whitePlayer.rating ? ` (${history.whitePlayer.rating})` : ""}`
      : "White",
    blackPlayerLabel: history.blackPlayer
      ? `${history.blackPlayer.displayName}${history.blackPlayer.rating ? ` (${history.blackPlayer.rating})` : ""}`
      : "Black",
  }

  const status = analysis?.status
  const showAnalyseCta = !status || status === "PENDING" || status === "FAILED"
  const showAnalysing = status === "QUEUED" || status === "RUNNING"

  return (
    <Layout>
      <div className="mb-3 flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
        <h1 className="text-xl font-semibold">
          {playerLabels.whitePlayerLabel} vs {playerLabels.blackPlayerLabel}
        </h1>
        <p className="text-sm text-muted-foreground">
          {outcomeLabel[history.outcome] ?? history.outcome}
        </p>
      </div>

      {showAnalyseCta && (
        <div className="mb-3 flex items-center gap-2">
          <Button
            onClick={() => requestAnalysis(matchId)}
            disabled={requestPending}
            size="sm"
          >
            {requestPending ? "Queueing..." : "Queue analysis"}
          </Button>
          <span className="text-sm text-muted-foreground">
            {status === "FAILED"
              ? "Previous analysis failed."
              : "This match hasn't been analysed yet."}
          </span>
        </div>
      )}
      {showAnalysing && (
        <div className="mb-3 flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner />
          {status === "QUEUED" ? "Queued for analysis..." : "Analysing..."}
        </div>
      )}

      <div className="flex w-full justify-center">
        <div className="flex w-full flex-col gap-4 md:w-11/12 md:flex-row lg:w-5/6">
          <div className="min-w-0 md:flex-1">
            <ChessBoard
              readOnly
              isPlayersTurn={false}
              isGameOver
              isCheck={false}
              boardState={displayedBoard}
              moveCallback={() => {}}
              whitePerspective
              pieceOrigins={EMPTY_PIECE_ORIGINS}
              playerLabels={playerLabels}
            />
          </div>
          <div className="w-full shrink-0 md:w-72 lg:w-80">
            <MatchAnalysisPanel
              moves={moves}
              selectedIndex={previewIndex}
              onSelect={setPreviewIndex}
              analyses={analysisMap}
            />
          </div>
        </div>
      </div>
      <BackArrowLink />
    </Layout>
  )
}
