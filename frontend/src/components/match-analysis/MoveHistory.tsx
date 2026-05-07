import { useEffect, useRef, type ReactNode } from "react"
import type { MoveHistoryEntry } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import { Table, TableBody, TableCell, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

export type AnalysisScore = { cp?: number; mate?: number }

export type Classification =
  | "CLASSIFICATION_BEST"
  | "CLASSIFICATION_GREAT"
  | "CLASSIFICATION_GOOD"
  | "CLASSIFICATION_OK"
  | "CLASSIFICATION_INACCURACY"
  | "CLASSIFICATION_MISTAKE"
  | "CLASSIFICATION_BLUNDER"

export const CLASSIFICATION_LABEL: Record<
  MoveAnalysis["classification"],
  string
> = {
  CLASSIFICATION_BEST: "(Best)",
  CLASSIFICATION_GREAT: "(Great)",
  CLASSIFICATION_GOOD: "(Good)",
  CLASSIFICATION_OK: "(Okay)",
  CLASSIFICATION_INACCURACY: "(Inaccurate)",
  CLASSIFICATION_MISTAKE: "(Mistake)",
  CLASSIFICATION_BLUNDER: "(Blunder)",
}

export const CLASSIFICATION_COLOUR: Record<
  MoveAnalysis["classification"],
  string
> = {
  CLASSIFICATION_BEST: "var(--color-classification-best)",
  CLASSIFICATION_GREAT: "var(--color-classification-great)",
  CLASSIFICATION_GOOD: "var(--color-classification-good)",
  CLASSIFICATION_OK: "var(--color-classification-ok)",
  CLASSIFICATION_INACCURACY: "var(--color-classification-inaccuracy)",
  CLASSIFICATION_MISTAKE: "var(--color-classification-mistake)",
  CLASSIFICATION_BLUNDER: "var(--color-classification-blunder)",
}

export type MoveAnalysis = {
  ply: number
  moveUci: string
  bestMoveUci: string
  scoreBefore: AnalysisScore
  scoreAfter: AnalysisScore
  cpLoss: number
  classification: Classification
  pv: string[]
  depth: number
}

export type MoveHistoryProps = {
  moves: MoveHistoryEntry[]
  selectedIndex: number | null
  onSelect: (index: number | null) => void
  analyses?: Map<number, MoveAnalysis>
  renderAnalysis?: (analysis: MoveAnalysis | undefined) => ReactNode
  className?: string
}

const MoveHistory = ({
  moves,
  selectedIndex,
  onSelect,
  analyses,
  renderAnalysis,
  className,
}: MoveHistoryProps) => {
  const scrollRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (selectedIndex !== null) return
    const el = scrollRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [moves.length, selectedIndex])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (
        e.key !== "ArrowLeft" &&
        e.key !== "ArrowRight" &&
        e.key !== "ArrowUp" &&
        e.key !== "ArrowDown"
      ) {
        return
      }
      const target = e.target as HTMLElement | null
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable)
      ) {
        return
      }
      if (moves.length === 0) return

      const liveIdx = moves.length - 1
      const current = selectedIndex ?? liveIdx
      if (e.key === "ArrowUp") {
        if (current === 0) return
        e.preventDefault()
        onSelect(0)
      } else if (e.key === "ArrowDown") {
        if (selectedIndex === null) return
        e.preventDefault()
        onSelect(null)
      } else if (e.key === "ArrowLeft") {
        if (current === 0) return
        e.preventDefault()
        onSelect(current - 1)
      } else {
        if (current >= liveIdx) return
        e.preventDefault()
        const next = current + 1
        onSelect(next === liveIdx ? null : next)
      }
    }
    window.addEventListener("keydown", handler)
    return () => window.removeEventListener("keydown", handler)
  }, [moves.length, selectedIndex, onSelect])

  if (moves.length === 0) {
    return (
      <div className={cn("text-sm text-muted-foreground", className)}>
        No moves yet.
      </div>
    )
  }

  const rounds: { white: number | null; black: number | null }[] = []
  for (let i = 0; i < moves.length; i += 2) {
    rounds.push({
      white: i,
      black: i + 1 < moves.length ? i + 1 : null,
    })
  }

  const renderMoveCell = (index: number | null) => {
    if (index === null) return <TableCell className="px-2 py-1" />
    const move = moves[index]
    const isSelected = index === selectedIndex
    const analysis = analyses?.get(index + 1)
    return (
      <TableCell
        role="button"
        tabIndex={0}
        onClick={() => onSelect(index)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault()
            onSelect(index)
          }
        }}
        className={cn(
          "cursor-pointer px-2 py-1 text-sm transition-colors hover:bg-accent/50",
          isSelected && "bg-accent font-semibold hover:bg-accent"
        )}
      >
        <span>{move.san}</span>
        {renderAnalysis && (
          <span className="ml-1 align-middle">{renderAnalysis(analysis)}</span>
        )}
      </TableCell>
    )
  }

  return (
    <div
      ref={scrollRef}
      className={cn("max-h-[60vh] overflow-y-auto", className)}
    >
      <Table>
        <TableBody>
          {rounds.map((round, r) => (
            <TableRow key={r} className="hover:bg-transparent">
              <TableCell className="w-10 px-2 py-1 text-xs text-muted-foreground tabular-nums">
                {r + 1}.
              </TableCell>
              {renderMoveCell(round.white)}
              {renderMoveCell(round.black)}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export default MoveHistory
