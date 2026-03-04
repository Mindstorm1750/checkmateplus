import { Button } from "@/components/ui/button"
import Layout from "../components/Layout"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../components/ui/dialog"
import { useRef, useState, type ComponentProps, type Dispatch, type SetStateAction } from "react"
import { useNavigate } from "react-router"
import { Client, type IMessage } from "@stomp/stompjs"
import SockJS from 'sockjs-client/dist/sockjs';
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest"
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent"
import { Spinner } from "@/components/ui/spinner"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { type TimeControl, TimeControlToStringMap } from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"

function App() {
  const navigate = useNavigate();

  return (
    <Layout>
      <div className="flex flex-col gap-4 items-center">
        <PlayHumanButton className='min-w-3xs' />
        <PlayComputerButton className='min-w-3xs' />
        <Button className='min-w-3xs' onClick={() => navigate('/settings')}>Settings</Button>
      </div>
    </Layout>)
}

/**
 * For now there aren't any options to customise, so we just give the two buttons of "join queue" or "cancel" in this dialog
 */
const PlayHumanButton = (props: ComponentProps<typeof Button>) => {
  const [inQueue, setInQueue] = useState<boolean>(false);
  const titleText = "Play against humans";
  const descriptionText = "Get matched with other players";
  const navigate = useNavigate();
  const stompClient = useRef<Client | null>(null);

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + 'matchmaking');

    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str: string) => console.log(str),
      onConnect: () => {
        // Subscribe to your specific user queue for status updates
        // client.subscribe('/user/queue/status', (message: IMessage) => {
        //   const body: QueueStatus = JSON.parse(message.body);
        // });

        // Subscribe to match found events
        client.subscribe('/user/queue/match', (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body);
          setInQueue(false); // Match found, no longer in queue, redirect to match page
          navigate(`/match`, { state: match })
        });

        joinQueue(); // send join request immediately after connecting
      },
      onStompError: (frame: { headers: { [x: string]: string; }; }) => {
        console.log({ error: frame });
      },
    });

    client.activate();
    stompClient.current = client;
  };

  const joinQueue = () => {
    if (stompClient.current?.connected) {
      const joinRequest: JoinRequest = { displayName: `UserWhoJoinedAt${Date.now()}`, matchType: 'CASUAL_PVP' }; // TODO use player's displayName (or playerId if types/classes have changed)
      stompClient.current.publish({
        destination: "/app/join",
        body: JSON.stringify(joinRequest)
      });
      setInQueue(true);
    }
  };

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button {...props}>{titleText}</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
          <DialogDescription className="text-sm">{descriptionText}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant='outline'>Cancel</Button>{/** TODO: make this button exit queue and disconnect */}
          </DialogClose>
          <Button onClick={connect} variant={inQueue ? 'secondary' : 'default'} disabled={inQueue}>
            {inQueue && <Spinner data-icon='inline-start' />}
            {inQueue ? 'Queueing...' : 'Join queue'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const PlayComputerButton = (props: ComponentProps<typeof Button>) => {
  const titleText = "Play against a computer";
  const [inQueue, setInQueue] = useState<boolean>(false);
  const [timeControl, setTimeControl] = useState<TimeControl>();
  const navigate = useNavigate();
  const stompClient = useRef<Client | null>(null);

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + 'matchmaking');

    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str: string) => console.log(str),
      onConnect: () => {
        // Subscribe to your specific user queue for status updates
        // client.subscribe('/user/queue/status', (message: IMessage) => {
        //   const body: QueueStatus = JSON.parse(message.body);
        // });

        // Subscribe to match found events
        client.subscribe('/user/queue/match', (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body);
          setInQueue(false); // Match found, no longer in queue, redirect to match page
          navigate(`/match`, { state: match })
        });

        joinQueue(); // send join request immediately after connecting
      },
      onStompError: () => {
      },
    });

    client.activate();
    stompClient.current = client;
  };

  const joinQueue = () => {
    if (stompClient.current?.connected) {
      const joinRequest: JoinRequest = { displayName: `UserWhoJoinedAt${Date.now()}`, matchType: 'PVE', botConfiguration: { botModelType: 'RANDOM', botMoveTimeModelType: 'MEDIAN' }, timeControl }; // TODO use player's displayName (or playerId if types/classes have changed)
      stompClient.current.publish({
        destination: "/app/join",
        body: JSON.stringify(joinRequest)
      });
      setInQueue(true);
    }
  };

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button {...props}>{titleText}</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
        </DialogHeader>
        <SelectTimeControl callBack={setTimeControl} />
        <DialogFooter>
          <DialogClose asChild>
            <Button variant='outline'>Cancel</Button>
          </DialogClose>
          <Button onClick={connect} disabled={inQueue}>Play</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export const SelectTimeControl = ({ callBack }: { callBack: Dispatch<SetStateAction<TimeControl | undefined>> }) => {
  return (<Select onValueChange={(timeControl) => callBack(timeControl as TimeControl)}>
    <SelectTrigger>
      <SelectValue placeholder='Select a time control' />
    </SelectTrigger>
    <SelectContent>
      <SelectGroup>
        {
          [...TimeControlToStringMap.entries()].map((entry) => {
            const [value, label] = entry;
            return <SelectItem key={value} value={value.toString()}>{label}</SelectItem>
          })
        }
      </SelectGroup>
    </SelectContent>
  </Select>);
}

export default App
