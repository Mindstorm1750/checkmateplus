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
  useRef,
  useState,
  type ComponentProps,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from "react"
import { useNavigate } from "react-router"
import { Client, type IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client/dist/sockjs"
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest"
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent"
import { Spinner } from "@/components/ui/spinner"
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
import { Bot, Eye, Flame, Info, Play } from "lucide-react"
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card"
import type { QueueStatus } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/QueueStatus"
import { useJwt } from "@/lib/JwtProvider"
import { ensureFreshJwt } from "@/lib/auth"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group.tsx"
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType.ts"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import { usePlayerStats } from "@/hooks/usePlayerStats"

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
      <div className="lg:flex">
        <div className="flex flex-col gap-7">
          {stats && stats.currentStreak >= 2 && (
            <div className="inline-flex w-fit items-center gap-2 rounded-full border border-border bg-foreground/4 px-3 py-1.5 text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
              <Flame className="size-3 text-emerald-400" />
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
        <div>test</div>
      </div>
    </Layout>
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
  const [inQueue, setInQueue] = useState<boolean>(false)
  const [queueSeconds, setQueueSeconds] = useState<number>(0)
  const queueTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const [timeControl, setTimeControl] = useState<
    TimeControl | "ANY" | undefined
  >(initialTimeControl)
  const [matchType, setMatchType] = useState<MatchType>("COMPETITIVE_PVP")
  const titleText = "Play against humans"
  const descriptionText = "Get matched with other players"
  const navigate = useNavigate()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isGuest, isPending: jwtIsPending } = useJwt()

  const formatQueueTime = (seconds: number) => {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${s.toString().padStart(2, "0")}`
  }

  const startQueueTimer = () => {
    if (queueTimerRef.current) {
      clearInterval(queueTimerRef.current)
    }
    setQueueSeconds(0)
    queueTimerRef.current = setInterval(() => {
      setQueueSeconds((prev) => prev + 1)
    }, 1000)
  }

  const stopQueueTimer = () => {
    if (queueTimerRef.current) {
      clearInterval(queueTimerRef.current)
      queueTimerRef.current = null
    }
    setQueueSeconds(0)
  }

  const connect = () => {
    const client = new Client({
      webSocketFactory: () =>
        new SockJS(import.meta.env.VITE_BASE_URL + "matchmaking"),
      beforeConnect: async () => {
        const fresh = (await ensureFreshJwt()) ?? jwt
        client.connectHeaders = { Authorization: `Bearer ${fresh}` }
      },
      debug: (str: string) => console.log(str),
      onConnect: () => {
        // Subscribe to your specific user queue for status updates
        client.subscribe("/user/queue/status", (message: IMessage) => {
          const body: QueueStatus = JSON.parse(message.body)
          console.log("/user/queue/status", { body })
        })

        // Subscribe to match found events
        client.subscribe("/user/queue/match", (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body)
          setInQueue(false) // Match found, no longer in queue, redirect to match page
          stopQueueTimer()
          navigate(`/match`, { state: match })
        })

        joinQueue() // send join request immediately after connecting
      },
      onStompError: (frame: { headers: { [x: string]: string } }) => {
        console.log({ error: frame })
      },
    })

    client.activate()
    stompClient.current = client
  }

  const joinQueue = () => {
    if (stompClient.current?.connected) {
      const joinRequest: JoinRequest = {
        displayName: getLocalStorage("displayName") ?? "",
        matchType: isGuest ? "CASUAL_PVP" : (matchType ?? "CASUAL_PVP"),
        timeControl: timeControl == "ANY" ? undefined : timeControl,
      }
      stompClient.current.publish({
        destination: "/app/join",
        body: JSON.stringify(joinRequest),
      })
      setInQueue(true)
      startQueueTimer()
    }
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
          <DialogClose asChild>
            <Button
              onClick={() => {
                stompClient.current?.deactivate()
                setInQueue(false)
              }}
              variant="outline"
            >
              Cancel
            </Button>
          </DialogClose>
          <Button
            onClick={connect}
            variant={inQueue ? "secondary" : "default"}
            disabled={inQueue || timeControl === undefined || jwtIsPending}
          >
            {inQueue && <Spinner data-icon="inline-start" />}
            {inQueue
              ? `Queueing... (${formatQueueTime(queueSeconds)})`
              : "Join queue"}
          </Button>
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
  const [inQueue, setInQueue] = useState<boolean>(false)
  // timeControl won't actually ever be "ANY" here because we don't set includeAny = true in the SelectTimeControl component
  const [timeControl, setTimeControl] = useState<TimeControl | "ANY">()
  const navigate = useNavigate()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isPending: jwtIsPending } = useJwt()

  const connect = () => {
    const client = new Client({
      webSocketFactory: () =>
        new SockJS(import.meta.env.VITE_BASE_URL + "matchmaking"),
      beforeConnect: async () => {
        const fresh = (await ensureFreshJwt()) ?? jwt
        client.connectHeaders = { Authorization: `Bearer ${fresh}` }
      },
      debug: (str: string) => console.log(str),
      onConnect: () => {
        // Subscribe to your specific user queue for status updates
        // client.subscribe('/user/queue/status', (message: IMessage) => {
        //   const body: QueueStatus = JSON.parse(message.body);
        // });

        // Subscribe to match found events
        client.subscribe("/user/queue/match", (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body)
          setInQueue(false) // Match found, no longer in queue, redirect to match page
          navigate(`/match`, { state: match })
        })

        joinQueue() // send join request immediately after connecting
      },
      // onStompError: (frame: { headers: { [x: string]: string; }; }) => {
      // },
    })

    client.activate()
    stompClient.current = client
  }

  const joinQueue = () => {
    if (stompClient.current?.connected) {
      // const randomMoveTimeModelType: BotConfiguration['botMoveTimeModelType'] = Math.random() > 0.5 ? 'REALISTIC' : 'MEDIAN';
      // un-comment for randomised botMoveTimeModelType
      const joinRequest: JoinRequest = {
        displayName: getLocalStorage("displayName") ?? "",
        matchType: "PVE",
        botConfiguration: {
          botModelType: "MINMAX",
          botMoveTimeModelType: "REALISTIC",
        },
        timeControl: timeControl == "ANY" ? undefined : timeControl,
      }
      stompClient.current.publish({
        destination: "/app/join",
        body: JSON.stringify(joinRequest),
      })
      setInQueue(true)
    }
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
          <DialogClose asChild>
            <Button
              onClick={() => {
                stompClient.current?.deactivate()
                setInQueue(false)
              }}
              variant="outline"
            >
              Cancel
            </Button>
          </DialogClose>
          <Button
            onClick={connect}
            variant={inQueue ? "secondary" : "default"}
            disabled={inQueue || timeControl === undefined || jwtIsPending}
          >
            {inQueue && <Spinner data-icon="inline-start" />}
            {inQueue ? "Joining..." : "Play"}
          </Button>
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
