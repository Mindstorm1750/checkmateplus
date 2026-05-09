import Layout from "@/components/Layout"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Spinner } from "@/components/ui/spinner"
import { useMatchHistory } from "@/hooks/useMatchHistory"
import { useRequestMatchAnalysis } from "@/hooks/useRequestMatchAnalysis"
import {
  usePlayerName,
  usePlayerRating,
  usePlayerStats,
} from "@/hooks/usePlayerStats"
import { TimeControlToStringMap } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import type { MatchResult } from "@/lib/playerServiceTypes/MatchResult"
import { useNavigate, useParams } from "react-router"
import { BackArrowLink } from "@/components/ui/back-arrow-link.tsx"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table.tsx"
import type { MatchSummary } from "@/lib/playerServiceTypes/MatchSummary.ts"
import { MatchDetailsDialog } from "@/components/match-details/MatchDetailsDialog"
import { useEffect, useState } from "react"
import { getLocalStorage } from "@/lib/local-storage.helpers"
import { formatDate, formatDuration } from "@/lib/match-format.helpers"

const resultColour: Record<MatchResult, string> = {
  WIN: "text-green-500",
  LOSS: "text-red-500",
  DRAW: "text-yellow-500",
  ABANDONED: "text-muted-foreground",
}

export default function Profile() {
  const { playerId } = useParams<{ playerId: string }>()
  const isOwnProfile = playerId === getLocalStorage("playerId")

  const {
    data: stats,
    isLoading: statsLoading,
    isError: statsError,
  } = usePlayerStats(playerId)

  const displayName = stats?.displayName

  const {
    data: historyPages,
    isLoading: historyLoading,
    isError: historyError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useMatchHistory(playerId)

  const matches = historyPages?.pages.flatMap((p) => p.matches) ?? []

  const [selectedMatchId, setSelectedMatchId] = useState<string | null>(null)
  const selectedMatch =
    matches.find((m) => m.matchId === selectedMatchId) ?? null

  const [toast, setToast] = useState<string | null>(null)
  const [toastVisible, setToastVisible] = useState(false)
  useEffect(() => {
    if (!toast) return
    setToastVisible(true)
    const fade = window.setTimeout(() => setToastVisible(false), 4500)
    const remove = window.setTimeout(() => setToast(null), 5000)
    return () => {
      window.clearTimeout(fade)
      window.clearTimeout(remove)
    }
  }, [toast])

  const { mutateAsync: requestAnalysis } = useRequestMatchAnalysis()
  const [queueing, setQueueing] = useState(false)

  const handleQueueAll = async () => {
    if (matches.length === 0) return
    setQueueing(true)
    try {
      const results = await Promise.allSettled(
        matches.map((m) => requestAnalysis(m.matchId))
      )
      const ok = results.filter((r) => r.status === "fulfilled").length
      const failed = results.length - ok
      setToast(
        failed === 0
          ? `Queued ${ok} match${ok === 1 ? "" : "es"} for analysis`
          : `Queued ${ok}, ${failed} failed`
      )
    } finally {
      setQueueing(false)
    }
  }

  return (
    <Layout>
      <div className="flex flex-col gap-6">
        {/* Header */}
        <div>
          <h2 className="text-2xl font-bold">{displayName}</h2>
          {stats && (
            <p className="text-sm text-muted-foreground">
              Member since {formatDate(stats.memberSinceEpochMs)}
            </p>
          )}
        </div>

        {/* Stats */}
        {statsLoading && <Spinner />}
        {statsError && (
          <p className="text-destructive">Failed to load stats.</p>
        )}
        {stats && (
          <>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <StatCard label="Rating" value={stats.skillRating ?? "—"} />
              <StatCard label="Total Games" value={stats.totalGames} />
              <StatCard label="Win Rate" value={`${stats.winRate}%`} />
              <StatCard label="Current Streak" value={stats.currentStreak} />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <StatCard label="Wins" value={stats.wins} />
              <StatCard label="Losses" value={stats.losses} />
              <StatCard label="Draws" value={stats.draws} />
            </div>

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
              <StatCard label="Wins as White" value={stats.winsAsWhite} />
              <StatCard label="Wins as Black" value={stats.winsAsBlack} />
              <StatCard label="Best Streak" value={stats.longestWinStreak} />
            </div>

            {stats.favouriteTimeControl && (
              <p className="text-sm text-muted-foreground">
                Favourite time control:{" "}
                <span className="font-medium text-foreground">
                  {TimeControlToStringMap.get(stats.favouriteTimeControl) ??
                    stats.favouriteTimeControl}
                </span>
              </p>
            )}
          </>
        )}

        {/* Match History */}
        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-2">
            <div>
              <CardTitle>Match History</CardTitle>
              <CardDescription>Your recent games</CardDescription>
            </div>
            {isOwnProfile && matches.length > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleQueueAll}
                disabled={queueing}
              >
                {queueing ? "Queueing..." : "Queue all matches for analysis"}
              </Button>
            )}
          </CardHeader>
          <CardContent>
            {toast && (
              <div
                className={[
                  "mb-2 rounded-md bg-primary px-3 py-2 text-center text-sm text-primary-foreground",
                  "transition-all duration-500",
                  toastVisible
                    ? "translate-y-0 opacity-100"
                    : "translate-y-2 opacity-0",
                ].join(" ")}
                role="status"
                aria-live="polite"
              >
                {toast}
              </div>
            )}
            {historyLoading && <Spinner />}
            {historyError && (
              <p className="text-destructive">Failed to load match history.</p>
            )}
            {matches.length === 0 && !historyLoading && (
              <p className="text-sm text-muted-foreground">
                No matches played yet.
              </p>
            )}
            {matches.length > 0 && (
              <div className="flex flex-col gap-2">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Result</TableHead>
                      <TableHead>Colour</TableHead>
                      <TableHead>Opponent</TableHead>
                      <TableHead>Time Control</TableHead>
                      <TableHead>Duration</TableHead>
                      <TableHead>Date</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {matches.map((match) => (
                      <MatchRow
                        key={match.matchId}
                        match={match}
                        onSelect={() => setSelectedMatchId(match.matchId)}
                      />
                    ))}
                  </TableBody>
                </Table>

                {hasNextPage && (
                  <Button
                    variant="outline"
                    onClick={() => fetchNextPage()}
                    disabled={isFetchingNextPage}
                  >
                    {isFetchingNextPage ? "Loading..." : "Load more"}
                  </Button>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
      <BackArrowLink />
      <MatchDetailsDialog
        match={selectedMatch}
        open={selectedMatchId !== null && selectedMatch !== null}
        onOpenChange={(open) => !open && setSelectedMatchId(null)}
      />
    </Layout>
  )
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="px-4 py-3">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-xl font-bold tabular-nums">{value}</p>
      </CardContent>
    </Card>
  )
}

function MatchRow({
  match,
  onSelect,
}: {
  match: MatchSummary
  onSelect: () => void
}) {
  const { data: opponent } = usePlayerRating(match.opponentId ?? null)
  const { data: opponentName } = usePlayerName(match.opponentId)
  const navigate = useNavigate()

  return (
    <TableRow
      key={match.matchId}
      onClick={onSelect}
      className="cursor-pointer hover:bg-accent/50"
    >
      <TableCell className={`font-semibold ${resultColour[match.result]}`}>
        {match.result}
      </TableCell>
      <TableCell className="">
        {match.colour === "WHITE" ? "White" : "Black"}
      </TableCell>
      <TableCell
        onClick={(e) => {
          e.stopPropagation()
          if (opponent) navigate(`/profile/${opponent.playerId}`)
        }}
      >
        {opponentName ? (
          <p
            className={`${opponent ? "hover:underline" : "text-muted-foreground"}`}
          >{`${opponentName.displayName}${opponent?.skillRating ? ` (${opponent.skillRating})` : ""}`}</p>
        ) : (
          "Loading..."
        )}
      </TableCell>
      <TableCell>
        {TimeControlToStringMap.get(match.timeControl) ?? match.timeControl}
      </TableCell>
      <TableCell>
        {match.gameDurationMs ? formatDuration(match.gameDurationMs) : "—"}
      </TableCell>
      <TableCell>{formatDate(match.createdAtEpochMs)}</TableCell>
    </TableRow>
  )
}
