import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import type { MoveHistoryEntry } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import { cn } from "@/lib/utils"
import MoveHistory, { type MoveAnalysis } from "./MoveHistory"

export type MatchAnalysisPanelProps = {
  moves: MoveHistoryEntry[]
  selectedIndex: number | null
  onSelect: (index: number | null) => void
  analyses?: Map<number, MoveAnalysis>
  className?: string
}

const MatchAnalysisPanel = ({
  moves,
  selectedIndex,
  onSelect,
  analyses,
  className,
}: MatchAnalysisPanelProps) => {
  const isPreviewing = selectedIndex !== null

  return (
    <Card className={cn("py-4", className)}>
      <CardHeader className="px-4">
        <CardTitle>Moves</CardTitle>
      </CardHeader>
      <CardContent className="px-4">
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
        />
      </CardContent>
    </Card>
  )
}

export default MatchAnalysisPanel
