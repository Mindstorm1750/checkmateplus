import { type ReactNode, useRef, useState } from "react"
import { useNavigate } from "react-router"
import { Spinner } from "@/components/ui/spinner.tsx"
import { formatQueueTime } from "@/lib/formatting.helpers.ts"
import { Button } from "@/components/ui/button.tsx"
import SockJS from "sockjs-client/dist/sockjs"
import { Client, type IMessage } from "@stomp/stompjs"
import type { QueueStatus } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/QueueStatus.ts"
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent.ts"
import type { MatchRedirectType } from "@/types/Match.type.ts"
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest.ts"
import { useJwt } from "@/lib/JwtProvider.tsx"

interface QueueButtonProps {
  joinRequest: JoinRequest
  onMatchFound?: () => void
  cancelTrigger?: (cancel: () => void) => ReactNode
  buttonText: string
  onQueueChange?: (inQueue: boolean) => void
}

export function QueueButton({
  joinRequest,
  onMatchFound,
  cancelTrigger,
  buttonText,
  onQueueChange,
}: QueueButtonProps) {
  const navigate = useNavigate()
  const { jwt, isPending: jwtIsPending } = useJwt()

  const [inQueue, setInQueue] = useState(false)
  const [queueSeconds, setQueueSeconds] = useState(0)
  const stompClient = useRef<Client | null>(null)
  const queueTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const startQueueTimer = () => {
    if (queueTimerRef.current) clearInterval(queueTimerRef.current)
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

  const joinQueue = (client: Client) => {
    client.publish({
      destination: "/app/join",
      body: JSON.stringify(joinRequest),
    })
    setInQueue(true)
    onQueueChange && onQueueChange(true)
    startQueueTimer()
  }

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + "matchmaking")
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${jwt}` },
      debug: (str) => console.log(str),
      onConnect: () => {
        client.subscribe("/user/queue/status", (message: IMessage) => {
          const body: QueueStatus = JSON.parse(message.body)
          console.log("/user/queue/status", { body })
        })
        client.subscribe("/user/queue/match", (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body)
          const matchRedirectState: MatchRedirectType = {
            matchFoundEvent: match,
            joinRequest,
          }
          setInQueue(false)
          onQueueChange && onQueueChange(false)
          stopQueueTimer()
          onMatchFound?.()
          navigate("/match", { state: matchRedirectState })
        })
        joinQueue(client)
      },
      onStompError: (frame) => console.log({ error: frame }),
    })
    client.activate()
    stompClient.current = client
  }

  const cancel = () => {
    stompClient.current?.deactivate()
    setInQueue(false)
    onQueueChange && onQueueChange(false)
    stopQueueTimer()
  }

  return (
    <>
      {inQueue && cancelTrigger?.(cancel)}
      <Button
        onClick={inQueue ? cancel : connect}
        variant={inQueue ? "secondary" : "default"}
        disabled={jwtIsPending}
      >
        {inQueue && <Spinner data-icon="inline-start" />}
        {inQueue
          ? `Queueing... (${formatQueueTime(queueSeconds)})`
          : buttonText}
      </Button>
    </>
  )
}
