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
import { usePlayerRating, usePlayerStats } from "@/hooks/usePlayerStats"
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

function formatDuration(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`
}

function formatDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

const resultColour: Record<MatchResult, string> = {
  WIN: "text-green-500",
  LOSS: "text-red-500",
  DRAW: "text-yellow-500",
  ABANDONED: "text-muted-foreground",
}

export default function Profile() {
  const { playerId } = useParams<{ playerId: string }>()

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
          <CardHeader>
            <CardTitle>Match History</CardTitle>
            <CardDescription>Your recent games</CardDescription>
          </CardHeader>
          <CardContent>
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
                      <MatchRow key={match.matchId} match={match} />
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

function MatchRow({ match }: { match: MatchSummary }) {
  const { data: opponent } = usePlayerRating(match.opponentId ?? null)
  const navigate = useNavigate()

  return (
    <TableRow key={match.matchId}>
      <TableCell className={`font-semibold ${resultColour[match.result]}`}>
        {match.result}
      </TableCell>
      <TableCell className="text-muted-foreground">
        {match.colour === "WHITE" ? "White" : "Black"}
      </TableCell>
      <TableCell
        className="cursor-pointer"
        onClick={() => opponent && navigate(`/profile/${opponent.playerId}`)}
      >
        {opponent ? (
          <p className="hover:underline">{`${opponent.displayName}${opponent.skillRating ? ` (${opponent.skillRating})` : ""}`}</p>
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
