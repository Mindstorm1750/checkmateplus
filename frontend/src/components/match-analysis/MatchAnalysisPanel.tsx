import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import type { MoveHistoryEntry } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import { cn } from "@/lib/utils"
import MoveHistory, {
  CLASSIFICATION_COLOUR,
  CLASSIFICATION_LABEL,
  type AnalysisScore,
  type Classification,
  type MoveAnalysis,
} from "./MoveHistory"
import { Fragment, useMemo, type ReactNode } from "react"

export type MatchAnalysisPanelProps = {
  moves: MoveHistoryEntry[]
  selectedIndex: number | null
  onSelect: (index: number | null) => void
  analyses?: Map<number, MoveAnalysis>
  className?: string
}

const CP_CLAMP = 1000

const scoreToPercent = (score: AnalysisScore | undefined): number | null => {
  if (!score) return null
  if (typeof score.mate === "number") {
    if (score.mate === 0) return 50
    return score.mate > 0 ? 100 : 0
  }
  if (typeof score.cp === "number") {
    const clamped = Math.max(-CP_CLAMP, Math.min(CP_CLAMP, score.cp))
    return ((clamped + CP_CLAMP) / (2 * CP_CLAMP)) * 100
  }
  return null
}

const formatScore = (score: AnalysisScore | undefined): string | null => {
  if (!score) return null
  if (typeof score.mate === "number") {
    if (score.mate === 0) return "0"
    const sign = score.mate > 0 ? "" : "−"
    return `${sign}M${Math.abs(score.mate)}`
  }
  if (typeof score.cp === "number") {
    const pawns = score.cp / 100
    const sign = pawns > 0 ? "+" : pawns < 0 ? "−" : ""
    return `${sign}${Math.abs(pawns).toFixed(2)}`
  }
  return null
}

const SUMMARY_ORDER: Classification[] = [
  "CLASSIFICATION_BEST",
  "CLASSIFICATION_GREAT",
  "CLASSIFICATION_GOOD",
  "CLASSIFICATION_OK",
  "CLASSIFICATION_INACCURACY",
  "CLASSIFICATION_MISTAKE",
  "CLASSIFICATION_BLUNDER",
]

type SideTally = Partial<Record<Classification, number>>

const MatchAnalysisPanel = ({
  moves,
  selectedIndex,
  onSelect,
  analyses,
  className,
}: MatchAnalysisPanelProps) => {
  const isPreviewing = selectedIndex !== null
  const hasAnalyses = !!analyses && analyses.size > 0

  const focusPly =
    selectedIndex !== null
      ? selectedIndex + 1
      : moves.length > 0
        ? moves.length
        : null
  const focusAnalysis =
    focusPly !== null ? analyses?.get(focusPly) : undefined

  const evalPercent = scoreToPercent(focusAnalysis?.scoreAfter)
  const evalLabel = formatScore(focusAnalysis?.scoreAfter)

  const showBestMove =
    !!focusAnalysis &&
    !!focusAnalysis.bestMoveUci &&
    focusAnalysis.moveUci !== focusAnalysis.bestMoveUci

  const summary = useMemo(() => {
    const white: SideTally = {}
    const black: SideTally = {}
    if (!analyses) return { white, black }
    analyses.forEach((a) => {
      const bucket = a.ply % 2 === 1 ? white : black
      bucket[a.classification] = (bucket[a.classification] ?? 0) + 1
    })
    return { white, black }
  }, [analyses])

  const summaryRows = SUMMARY_ORDER.filter(
    (c) => (summary.white[c] ?? 0) + (summary.black[c] ?? 0) > 0
  )

  function renderAnalysis(analysis: MoveAnalysis | undefined): ReactNode {
    return (
      <p
        style={{
          color: analysis?.classification
            ? CLASSIFICATION_COLOUR[analysis?.classification]
            : "",
        }}
      >
        {analysis?.classification
          ? CLASSIFICATION_LABEL[analysis?.classification]
          : ""}
      </p>
    )
  }

  return (
    <Card className={cn("py-4", className)}>
      <CardHeader className="px-4">
        <CardTitle>Moves</CardTitle>
      </CardHeader>
      <CardContent className="px-4">
        {hasAnalyses && (
          <div className="mb-3 space-y-2">
            {evalPercent !== null && (
              <div className="flex items-center gap-2">
                <div className="h-2 flex-1 overflow-hidden rounded border bg-black">
                  <div
                    className="h-full bg-white"
                    style={{ width: `${evalPercent}%` }}
                  />
                </div>
                {evalLabel && (
                  <span className="w-12 text-right text-xs font-mono tabular-nums text-muted-foreground">
                    {evalLabel}
                  </span>
                )}
              </div>
            )}
            {showBestMove && (
              <p className="text-xs text-muted-foreground">
                Best:{" "}
                <code className="font-mono">{focusAnalysis!.bestMoveUci}</code>
              </p>
            )}
            {summaryRows.length > 0 && (
              <div className="grid grid-cols-[1fr_auto_auto] gap-x-3 gap-y-0.5 text-xs">
                <span />
                <span className="text-right text-muted-foreground">White</span>
                <span className="text-right text-muted-foreground">Black</span>
                {summaryRows.map((c) => (
                  <Fragment key={c}>
                    <span style={{ color: CLASSIFICATION_COLOUR[c] }}>
                      {CLASSIFICATION_LABEL[c].replace(/[()]/g, "")}
                    </span>
                    <span className="text-right tabular-nums">
                      {summary.white[c] ?? 0}
                    </span>
                    <span className="text-right tabular-nums">
                      {summary.black[c] ?? 0}
                    </span>
                  </Fragment>
                ))}
              </div>
            )}
          </div>
        )}
        {isPreviewing && (
          <div className="mb-2 flex justify-end">
            <Button size="sm" variant="outline" onClick={() => onSelect(null)}>
              Return to live
            </Button>
          </div>
        )}
        <MoveHistory
          moves={moves}
          selectedIndex={selectedIndex}
          onSelect={onSelect}
          analyses={analyses}
          renderAnalysis={renderAnalysis}
        />
      </CardContent>
    </Card>
  )
}

export default MatchAnalysisPanel
