import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Spinner } from "@/components/ui/spinner"
import {
  useMatchAnalysis,
  type AnalysisStatus,
  type MatchAnalysisResponse,
} from "@/hooks/useMatchAnalysis"
import { useRequestMatchAnalysis } from "@/hooks/useRequestMatchAnalysis"
import { usePlayerRating } from "@/hooks/usePlayerStats"
import { formatDate, formatDuration } from "@/lib/match-format.helpers"
import type { MatchSummary } from "@/lib/playerServiceTypes/MatchSummary"
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType"
import { TimeControlToStringMap } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import { useNavigate } from "react-router"

const matchTypeLabel: Record<MatchType, string> = {
  CASUAL_PVP: "Casual",
  COMPETITIVE_PVP: "Ranked",
  PVE: "vs Bot",
}

type Props = {
  match: MatchSummary | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function MatchDetailsDialog({ match, open, onOpenChange }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[90dvh] flex-col overflow-y-auto sm:max-w-2xl">
        {match && <Body match={match} />}
      </DialogContent>
    </Dialog>
  )
}

function Body({ match }: { match: MatchSummary }) {
  const navigate = useNavigate()
  const { data: opponent } = usePlayerRating(match.opponentId ?? null)
  const { data: analysis, isPending: analysisPending } = useMatchAnalysis(
    match.matchId
  )
  const { mutate: requestAnalysis, isPending: requestPending } =
    useRequestMatchAnalysis()

  const status = analysis?.status
  const canAnalyse =
    !analysisPending &&
    status !== "QUEUED" &&
    status !== "RUNNING" &&
    status !== "COMPLETE"

  return (
    <>
      <DialogHeader>
        <DialogTitle>
          You vs {opponent?.displayName ?? "Opponent"}
          {opponent?.skillRating ? ` (${opponent.skillRating})` : ""}
        </DialogTitle>
      </DialogHeader>

      <AnalysisStatusPanel
        analysis={analysis ?? null}
        isPending={analysisPending}
      />

      <div className="grid grid-cols-2 gap-3 border-t pt-3 text-sm sm:grid-cols-3">
        <Detail label="Result" value={match.result} />
        <Detail
          label="Colour"
          value={match.colour === "WHITE" ? "White" : "Black"}
        />
        <Detail label="Type" value={matchTypeLabel[match.matchType]} />
        <Detail
          label="Time control"
          value={
            TimeControlToStringMap.get(match.timeControl) ?? match.timeControl
          }
        />
        <Detail
          label="Duration"
          value={
            match.gameDurationMs ? formatDuration(match.gameDurationMs) : "—"
          }
        />
        <Detail label="Date" value={formatDate(match.createdAtEpochMs)} />
      </div>

      <div className="flex flex-wrap justify-end gap-2 border-t pt-3">
        <Button
          variant="outline"
          disabled={!canAnalyse || requestPending}
          onClick={() => requestAnalysis(match.matchId)}
        >
          {requestPending ? "Queueing..." : "Analyse this match"}
        </Button>
        <Button onClick={() => navigate(`/match/${match.matchId}/review`)}>
          Review match
        </Button>
      </div>
    </>
  )
}

function AnalysisStatusPanel({
  analysis,
  isPending,
}: {
  analysis: MatchAnalysisResponse | null
  isPending: boolean
}) {
  if (isPending) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Spinner /> Loading analysis status...
      </div>
    )
  }

  const status: AnalysisStatus = analysis?.status ?? "PENDING"

  if (status === "QUEUED" || status === "RUNNING") {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Spinner />
        {status === "QUEUED" ? "Queued for analysis" : "Analysis in progress"}
      </div>
    )
  }

  if (status === "COMPLETE" && analysis) {
    const blunders = analysis.moves.filter(
      (m) => m.classification === "CLASSIFICATION_BLUNDER"
    ).length
    const mistakes = analysis.moves.filter(
      (m) => m.classification === "CLASSIFICATION_MISTAKE"
    ).length
    return (
      <div className="text-sm">
        <p className="font-medium text-foreground">Analysis complete</p>
        <p className="text-muted-foreground">
          {analysis.moves.length} plies · {blunders} blunder
          {blunders === 1 ? "" : "s"} · {mistakes} mistake
          {mistakes === 1 ? "" : "s"}
        </p>
      </div>
    )
  }

  if (status === "FAILED") {
    return (
      <p className="text-sm text-destructive">
        Analysis failed. Try queueing again.
      </p>
    )
  }

  if (status === "DISABLED") {
    return (
      <p className="text-sm text-muted-foreground">
        Analysis is disabled on this server.
      </p>
    )
  }

  return (
    <p className="text-sm text-muted-foreground">
      This match hasn't been analysed yet.
    </p>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-medium">{value}</p>
    </div>
  )
}
