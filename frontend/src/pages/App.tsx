import { Button } from "@/components/ui/button"
import Layout from "../components/Layout"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../components/ui/dialog"
import {
  useState,
  type ComponentProps,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from "react"
import { useNavigate } from "react-router"
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  type TimeControl,
  TimeControlToStringMap,
} from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import { ArrowRight, Bot, Eye, Flame, Info, Play, Trophy } from "lucide-react"
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card"
import { useJwt } from "@/lib/JwtProvider"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group.tsx"
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType.ts"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import { usePlayerRating, usePlayerStats } from "@/hooks/usePlayerStats"
import { useLiveMatches } from "@/hooks/useLiveMatches"
import MiniChessBoard from "@/components/ui/MiniChessBoard"
import { fenToBoard, STARTING_FEN } from "@/lib/chess.helpers"
import type {
  SpectateMatch,
  SpectatePlayer,
} from "@/lib/playerServiceTypes/SpectateMatch"
import { QueueButton } from "@/components/QueueButton.tsx"
import { spectateMatchFilter } from "@/lib/spectate-match-filter.ts"
import { useLeaderboard } from "@/hooks/useLeaderboard"
import { Spinner } from "@/components/ui/spinner"

const QUICK_TIME_CONTROLS: { name: string; tc: TimeControl; label: string }[] =
  [
    { name: "Bullet", tc: "BULLET_1_0", label: "1+0" },
    { name: "Blitz", tc: "BLITZ_3_0", label: "3+0" },
    { name: "Rapid", tc: "RAPID_10_0", label: "10+0" },
    { name: "Classical", tc: "CLASSICAL_30_0", label: "30+0" },
  ]

function App() {
  const navigate = useNavigate()
  const { isGuest } = useJwt()
  const playerId = getLocalStorage("playerId")
  const displayName = getLocalStorage("displayName") ?? "friend"
  const { data: stats } = usePlayerStats(isGuest ? null : playerId)
  const { data: liveData, isLoading: liveLoading } = useLiveMatches(1)
  const topMatch: SpectateMatch | undefined =
    liveData?.pages[0]?.matches &&
    spectateMatchFilter(liveData.pages[0].matches, playerId)[0]
  const showLive = !liveLoading && Boolean(topMatch)

  return (
    <Layout>
      <div className="lg:flex lg:items-start lg:gap-12">
        <div className="flex flex-col gap-7 lg:max-w-md lg:flex-1">
          {stats && stats.currentStreak >= 2 && (
            <div className="inline-flex w-fit items-center gap-2 rounded-full border border-border bg-foreground/4 px-3 py-1.5 text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
              <Flame className="size-3 text-brand-accent" />
              You're on a {stats.currentStreak}-game win streak
            </div>
          )}

          <div>
            <h1 className="font-serif text-5xl leading-[1.02] tracking-tight md:text-6xl">
              <b>{isGuest ? "Welcome," : "Welcome back,"}</b>
              <br />
              <span className="truncate text-muted-foreground italic">
                {displayName}.
              </span>
            </h1>
            {!isGuest && stats && (
              <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
                Rating{" "}
                <span className="font-semibold text-foreground">
                  {stats.skillRating ?? "—"}
                </span>{" "}
                ·{" "}
                <span className="font-semibold text-foreground">
                  {stats.totalGames}
                </span>{" "}
                games played
                {stats.totalGames > 0 && (
                  <>
                    {" · "}
                    <span className="font-semibold text-foreground">
                      {Math.round(stats.winRate)}%
                    </span>{" "}
                    win rate
                  </>
                )}
              </p>
            )}
          </div>

          <PlayHumanButton
            trigger={
              <Button className="min-h-12">
                <Play className="size-5 fill-current" />
                Play against humans
              </Button>
            }
          />

          <div className="grid grid-cols-2 gap-3">
            <PlayComputerButton
              trigger={
                <Button variant="outline">
                  <Bot className="size-4" />
                  Play computer
                </Button>
              }
            />
            <Button onClick={() => navigate("/spectate")} variant="outline">
              <Eye className="size-4" />
              Spectate
            </Button>
          </div>

          <div>
            <div className="mb-2.5 text-[11px] font-semibold tracking-wider text-muted-foreground uppercase">
              Quick play · time control
            </div>
            <div className="grid grid-cols-4 gap-2">
              {QUICK_TIME_CONTROLS.map((tc) => (
                <PlayHumanButton
                  key={tc.tc}
                  initialTimeControl={tc.tc}
                  trigger={
                    <button
                      type="button"
                      className="flex cursor-pointer flex-col items-start gap-0.5 rounded-lg border border-border bg-foreground/3 px-3 py-3.5 text-left transition-all hover:bg-foreground/6"
                    >
                      <span className="text-[11px] font-medium text-muted-foreground">
                        {tc.name}
                      </span>
                      <span className="text-lg font-bold text-foreground tabular-nums">
                        {tc.label}
                      </span>
                    </button>
                  }
                />
              ))}
            </div>
          </div>
        </div>
        {!liveLoading &&
          (topMatch ? (
            <LiveHeroBoardCard match={topMatch} />
          ) : (
            <LeaderboardCard className="mt-8 lg:mt-0 lg:flex-1 lg:self-stretch" />
          ))}
      </div>
      {showLive && <LeaderboardCard className="mt-12 lg:max-w-md" />}
    </Layout>
  )
}

const formatClock = (seconds: number): string => {
  const safe = Math.max(0, Math.floor(seconds))
  const m = Math.floor(safe / 60)
  const s = safe % 60
  return `${m}:${s.toString().padStart(2, "0")}`
}

const labelForPlayer = (player?: SpectatePlayer): string => {
  if (!player) return "—"
  if (player.type === "ANONYMOUS") return player.displayName || "Guest"
  return player.displayName
}

function LiveHeroBoardCard({ match }: { match: SpectateMatch }) {
  const navigate = useNavigate()
  const board = fenToBoard(match.lastFen ?? STARTING_FEN)

  return (
    <div className="mt-8 ml-auto flex flex-col gap-3.5 rounded-2xl border border-border bg-card p-5 lg:mt-0 lg:w-96 lg:shrink-0">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded bg-destructive px-2 py-0.5 text-[10px] font-bold tracking-widest text-white uppercase">
            <span className="size-1.5 animate-pulse rounded-full bg-white" />
            Live
          </span>
          <span className="text-xs text-muted-foreground">Top board</span>
        </div>
        <span className="text-xs text-muted-foreground tabular-nums">
          Move {match.moveNumber}
        </span>
      </div>

      <PlayerTimerRow
        player={match.blackPlayer}
        seconds={match.blackTimeSeconds}
      />

      <MiniChessBoard boardState={board} />

      <PlayerTimerRow
        player={match.whitePlayer}
        seconds={match.whiteTimeSeconds}
      />

      <button
        type="button"
        onClick={() => navigate(`/spectate/${match.matchId}`, { state: match })}
        className="mt-1 flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-border text-sm font-medium text-foreground transition-colors hover:bg-muted"
      >
        Watch full game
        <ArrowRight className="size-4" />
      </button>
    </div>
  )
}

function LeaderboardCard({ className = "" }: { className?: string }) {
  const navigate = useNavigate()
  const { data, isLoading, isError } = useLeaderboard(10)

  return (
    <div
      className={`flex flex-col gap-4 rounded-2xl border border-border bg-card p-6 ${className}`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded bg-foreground/10 px-2 py-0.5 text-[10px] font-bold tracking-widest text-foreground uppercase">
            <Trophy className="size-3" />
            Top 10
          </span>
          <span className="text-xs text-muted-foreground">Leaderboard</span>
        </div>
      </div>

      {isLoading && (
        <div className="flex justify-center py-10">
          <Spinner />
        </div>
      )}
      {isError && (
        <p className="text-sm text-destructive">Failed to load leaderboard.</p>
      )}
      {data && (
        <ul className="flex flex-1 flex-col">
          {data.slice(0, 10).map((entry, idx) => (
            <li
              key={entry.playerId}
              className={`flex items-center justify-between py-2.5 text-sm ${
                idx > 0 ? "border-t border-border/60" : ""
              }`}
            >
              <div className="flex min-w-0 items-center gap-3">
                <span className="w-6 text-right text-muted-foreground tabular-nums">
                  {entry.rank}
                </span>
                <span
                  className="cursor-pointer truncate font-medium hover:underline"
                  onClick={() => navigate(`/profile/${entry.playerId}`)}
                >
                  {entry.displayName}
                </span>
              </div>
              <span className="font-semibold tabular-nums">
                {entry.skillRating}
              </span>
            </li>
          ))}
        </ul>
      )}

      <button
        type="button"
        onClick={() => navigate("/leaderboard")}
        className="mt-auto flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-border text-sm font-medium text-foreground transition-colors hover:bg-muted"
      >
        View full leaderboard
        <ArrowRight className="size-4" />
      </button>
    </div>
  )
}

function PlayerTimerRow({
  player,
  seconds,
}: {
  player?: SpectatePlayer
  seconds: number
}) {
  const { data: playerData } = usePlayerRating(player?.playerId ?? null)
  return (
    <div className="flex items-center justify-between text-sm">
      <div>
        <div className="font-semibold">{labelForPlayer(player)}</div>
        <div className="text-xs text-muted-foreground tabular-nums">
          {playerData?.skillRating && `Rating: ${playerData.skillRating}`}
        </div>
      </div>
      <div className="rounded-md bg-muted px-3 py-1.5 text-base font-bold tabular-nums">
        {formatClock(seconds)}
      </div>
    </div>
  )
}

type PlayHumanButtonProps = ComponentProps<typeof Button> & {
  trigger?: ReactNode
  initialTimeControl?: TimeControl
}

const PlayHumanButton = ({
  trigger,
  initialTimeControl,
  ...buttonProps
}: PlayHumanButtonProps) => {
  const [timeControl, setTimeControl] = useState<
    TimeControl | "ANY" | undefined
  >(initialTimeControl)
  const { isGuest } = useJwt()
  const [matchType, setMatchType] = useState<MatchType>(
    isGuest ? "CASUAL_PVP" : "COMPETITIVE_PVP"
  )
  const [inQueue, setInQueue] = useState(false)
  const titleText = "Play against humans"
  const descriptionText = "Get matched with other players"
  const joinRequest: JoinRequest = {
    displayName: getLocalStorage("displayName") ?? "",
    matchType: isGuest ? "CASUAL_PVP" : (matchType ?? "CASUAL_PVP"),
    timeControl: timeControl == "ANY" ? undefined : timeControl,
  }

  return (
    <Dialog>
      <DialogTrigger asChild>
        {trigger ?? <Button {...buttonProps}>{titleText}</Button>}
      </DialogTrigger>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
          <DialogDescription className="text-sm">
            {descriptionText}
          </DialogDescription>
        </DialogHeader>

        <ToggleGroup
          type="single"
          variant="outline"
          value={matchType}
          onValueChange={(e) => setMatchType(e as MatchType)}
          defaultValue={isGuest ? "CASUAL_PVP" : "COMPETITIVE_PVP"}
        >
          <ToggleGroupItem value="CASUAL_PVP">Casual</ToggleGroupItem>
          {isGuest ? (
            <HoverCard>
              <HoverCardTrigger asChild>
                <span className="animate-[wiggle_1s_ease-in-out_0.2s_1_forwards] cursor-not-allowed">
                  <ToggleGroupItem
                    className="pointer-events-none rounded-l-none! border-l-0!"
                    disabled={true}
                    value="COMPETITIVE_PVP"
                  >
                    Ranked
                  </ToggleGroupItem>
                </span>
              </HoverCardTrigger>
              <HoverCardContent>
                <p className="text-sm">
                  Create an account to play ranked games.
                </p>
              </HoverCardContent>
            </HoverCard>
          ) : (
            <ToggleGroupItem value="COMPETITIVE_PVP">Ranked</ToggleGroupItem>
          )}
        </ToggleGroup>
        <SelectTimeControl
          value={timeControl}
          callBack={setTimeControl}
          includeAny
        />
        <DialogFooter>
          {!inQueue && (
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
          )}
          <QueueButton
            buttonText="Join queue"
            joinRequest={joinRequest}
            onQueueChange={setInQueue}
            cancelTrigger={(onClick) => (
              <DialogClose asChild>
                <Button onClick={onClick} variant="outline">
                  Cancel
                </Button>
              </DialogClose>
            )}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

type PlayComputerButtonProps = ComponentProps<typeof Button> & {
  trigger?: ReactNode
}

const PlayComputerButton = ({
  trigger,
  ...buttonProps
}: PlayComputerButtonProps) => {
  const titleText = "Play against a computer"
  // timeControl won't actually ever be "ANY" here because we don't set includeAny = true in the SelectTimeControl component
  const [timeControl, setTimeControl] = useState<TimeControl | "ANY">()
  const [inQueue, setInQueue] = useState(false)
  const joinRequest: JoinRequest = {
    displayName: getLocalStorage("displayName") ?? "",
    matchType: "PVE",
    botConfiguration: {
      botModelType: "MINMAX",
      botMoveTimeModelType: "REALISTIC",
    },
    timeControl: timeControl == "ANY" ? undefined : timeControl,
  }

  return (
    <Dialog>
      <DialogTrigger asChild>
        {trigger ?? <Button {...buttonProps}>{titleText}</Button>}
      </DialogTrigger>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
        </DialogHeader>
        <SelectTimeControl value={timeControl} callBack={setTimeControl} />
        <DialogFooter>
          {!inQueue && (
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
          )}
          <QueueButton
            joinRequest={joinRequest}
            onQueueChange={setInQueue}
            buttonText="Play"
            cancelTrigger={(onClick) => (
              <DialogClose asChild>
                <Button onClick={onClick} variant="outline">
                  Cancel
                </Button>
              </DialogClose>
            )}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export const SelectTimeControl = ({
  callBack,
  includeAny = false,
  value,
}: {
  callBack: Dispatch<SetStateAction<TimeControl | "ANY" | undefined>>
  includeAny?: boolean
  value: TimeControl | "ANY" | undefined
}) => {
  return (
    <div className="flex items-center gap-2">
      <Select
        value={value}
        onValueChange={(timeControl) => callBack(timeControl as TimeControl)}
      >
        <SelectTrigger>
          <SelectValue placeholder="Select a time control" />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {includeAny && (
              <SelectItem key="ANY" value="ANY">
                Any
              </SelectItem>
            )}
            {[...TimeControlToStringMap.entries()].map((entry) => {
              const [value, label] = entry
              return (
                <SelectItem key={value} value={value.toString()}>
                  {label}
                </SelectItem>
              )
            })}
          </SelectGroup>
        </SelectContent>
      </Select>
      <HoverCard>
        <HoverCardTrigger>
          <Info />
        </HoverCardTrigger>
        <HoverCardContent>
          <div className="text-sm">
            <p>Time controls are shown as X+Y:</p>
            <p>X minutes per player, with Y seconds added after each move.</p>
          </div>
        </HoverCardContent>
      </HoverCard>
    </div>
  )
}

export default App
