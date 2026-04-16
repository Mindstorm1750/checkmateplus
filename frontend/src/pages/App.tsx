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
import { Info } from "lucide-react"
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card"
import type { QueueStatus } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/QueueStatus"
import { useJwt } from "@/lib/JwtProvider"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group.tsx"
import type { MatchType } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType.ts"
import { getLocalStorage, isGuest } from "@/lib/local-storage.helpers.ts"

function App() {
  const navigate = useNavigate()

  return (
    <Layout>
      <div className="flex flex-col items-center gap-4">
        <PlayHumanButton className="min-w-3xs" />
        <PlayComputerButton className="min-w-3xs" />
        <Button className="min-w-3xs" onClick={() => navigate("/settings")}>
          Settings
        </Button>
      </div>
    </Layout>
  )
}

const PlayHumanButton = (props: ComponentProps<typeof Button>) => {
  const [inQueue, setInQueue] = useState<boolean>(false)
  const [timeControl, setTimeControl] = useState<TimeControl | "ANY">()
  const [matchType, setMatchType] = useState<MatchType>("COMPETITIVE_PVP")
  const titleText = "Play against humans"
  const descriptionText = "Get matched with other players"
  const navigate = useNavigate()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isPending: jwtIsPending } = useJwt()

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + "matchmaking")
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${jwt}`,
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
        matchType: isGuest() ? "CASUAL_PVP" : (matchType ?? "CASUAL_PVP"),
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
        <Button {...props}>{titleText}</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
          <DialogDescription className="text-sm">
            {descriptionText}
          </DialogDescription>
        </DialogHeader>
        {!isGuest() && (
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
            {inQueue ? "Queueing..." : "Join queue"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const PlayComputerButton = (props: ComponentProps<typeof Button>) => {
  const titleText = "Play against a computer"
  const [inQueue, setInQueue] = useState<boolean>(false)
  // timeControl won't actually ever be "ANY" here because we don't set includeAny = true in the SelectTimeControl component
  const [timeControl, setTimeControl] = useState<TimeControl | "ANY">()
  const navigate = useNavigate()
  const stompClient = useRef<Client | null>(null)
  const { jwt, isPending: jwtIsPending } = useJwt()

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + "matchmaking")

    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${jwt}`,
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
        <Button {...props}>{titleText}</Button>
      </DialogTrigger>
      <DialogContent>
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
