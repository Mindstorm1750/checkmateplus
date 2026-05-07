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
import { ArrowRight, Bot, Eye, Flame, Info, Play } from "lucide-react"
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
import { MmrChart } from "../components/mmrChart"
import { QueueButton } from "@/components/QueueButton.tsx"
import { spectateMatchFilter } from "@/lib/spectate-match-filter.ts"

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
              <button
                type="button"
                className="flex h-14 w-full cursor-pointer items-center justify-center gap-3 rounded-xl bg-primary px-5 text-base font-bold text-primary-foreground transition-all hover:bg-primary/90"
              >
                <Play className="size-5 fill-current" />
                Play against humans
              </button>
            }
          />

          <div className="grid grid-cols-2 gap-3">
            <PlayComputerButton
              trigger={
                <button
                  type="button"
                  className="flex h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg border border-border bg-muted text-sm font-medium text-foreground transition-all hover:bg-muted/80"
                >
                  <Bot className="size-4" />
                  Play computer
                </button>
              }
            />
            <button
              type="button"
              onClick={() => navigate("/spectate")}
              className="flex h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg border border-border bg-muted text-sm font-medium text-foreground transition-all hover:bg-muted/80"
            >
              <Eye className="size-4" />
              Spectate
            </button>
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
                      className="flex cursor-pointer flex-col items-start gap-0.5 rounded-lg border border-border bg-foreground/[0.03] px-3 py-3.5 text-left transition-all hover:bg-foreground/[0.06]"
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
        <LiveHeroBoardCard />
      </div>
      {!isGuest && (
        <div className="mt-12">
          <MmrChart />
        </div>
      )}
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

function LiveHeroBoardCard() {
  const navigate = useNavigate()
  const { data, isLoading, isError } = useLiveMatches(1)
  const ourPlayerId = getLocalStorage("playerId")
  const match: SpectateMatch | undefined =
    data?.pages[0]?.matches &&
    spectateMatchFilter(data?.pages[0]?.matches, ourPlayerId)[0]

  if (isLoading || isError || !match) return null

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

function PlayerTimerRow({
  player,
  seconds,
}: {
  player?: SpectatePlayer
  seconds: number
}) {
  const { data: playerData } = usePlayerRating(player?.playerId)
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
  const [matchType, setMatchType] = useState<MatchType>("COMPETITIVE_PVP")
  const [inQueue, setInQueue] = useState(false)
  const titleText = "Play against humans"
  const descriptionText = "Get matched with other players"
  const { isGuest } = useJwt()
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
        {!isGuest && (
          <ToggleGroup
            type="single"
            variant="outline"
            value={matchType}
            onValueChange={(e) => setMatchType(e as MatchType)}
            defaultValue="COMPETITIVE_PVP"
          >
            <ToggleGroupItem value="CASUAL_PVP">Casual</ToggleGroupItem>
            <ToggleGroupItem value="COMPETITIVE_PVP">Ranked</ToggleGroupItem>
          </ToggleGroup>
        )}
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
  includeAny?: Boolean
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
