import Layout from "@/components/Layout"
import { BackArrowLink } from "@/components/ui/back-arrow-link"
import ChessBoard, { type ChessBoardProps, type Usernames } from "@/components/ui/ChessBoard"
import {
  algebraicToSquare,
  fenToBoard,
  squareToAlgebraic,
} from "@/lib/chess.helpers"
import { usePreferences } from "@/lib/PreferencesProvider"
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage"
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage"
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import type { MoveMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MoveMessage"
import type { TimeUpdateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/TimeUpdateMessage"
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent"
import {
  EMPTY_BOARD,
  EMPTY_PIECE_ORIGINS,
  type Board,
  type Move,
  type Square,
} from "@/types/Chess.type"
import { Client, type IMessage } from "@stomp/stompjs"
import { useEffect, useRef, useState } from "react"
import { useLocation, useNavigate } from "react-router"
import SockJS from "sockjs-client/dist/sockjs"
import { useJwt } from "@/lib/JwtProvider"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog.tsx"
import { Button } from "@/components/ui/button.tsx"
import { Textarea } from "@/components/ui/textarea.tsx"
import { Star } from "lucide-react"
import { usePostGameSurvey } from "@/hooks/usePostGameSurvey.ts"
import type {DrawOfferMessage} from "@/lib/stompMessageTypes/GameServer/models/messages/DrawOfferMessage.ts";
import type {DrawOfferState} from "@/types/Match.type.ts";
import type {DrawResponseMessage} from "@/lib/stompMessageTypes/GameServer/models/messages/DrawResponseMessage.ts";


function Match() {
    const navigate = useNavigate();
    /**
     * using browser History state property to store the match information.
     * Might be a temporary solution OR copy this data to a more persistent form like localStorage.
     */
    const { state }: { state: MatchFoundEvent } = useLocation();
    useEffect(() => {
        if(!state) {
            // redirect to main page if we've visited this page directly instead of going through matchmaking
            navigate('/');
        }
    }, []);
    if (!state) {
        return null; // don't continue rendering if we haven't been redirected to this match page from matchmaking
    }
    const { matchId, websocketUrl, playerId } = state; // TODO make playerId persistent
    const [boardState, setBoardState] = useState<Board>(EMPTY_BOARD);
    const [lastMove, setLastMove] = useState<Move>();
    const [isCheck, setIsCheck] = useState<boolean>(false);
    /**
     * pieceOrigins is used in ChessBoard to animate pieces via framer motion's layoutId. 
     * Identifies pieces as they move from boardState to boardState.
     */
    const [pieceOrigins, setPieceOrigins] = useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS);
    const [clockState, setClockState] = useState<ChessBoardProps['clockState']>();
    const [isPlayersTurn, setIsPlayersTurn] = useState<boolean>(false);
    const [isPlayerWhite, setIsPlayerWhite] = useState<boolean>(true);
    const [isGameOver, setIsGameOver] = useState<boolean>(false);
    const [gameOverMessage, setGameOverMessage] = useState<String>();
    const [resignConfirmationDialog, setResignConfirmationDialog] = useState<boolean>(false);
    const [drawConfirmationDialog, setDrawConfirmationDialog] = useState<boolean>(false);
    const [drawOfferState, setDrawOfferState] = useState<DrawOfferState>('noOffer')
    const { preferences } = usePreferences();
    const stompClient = useRef<Client | null>(null);
    const { jwt, isPending: jwtIsPending } = useJwt();
    const [usernames, setUsernames] = useState<Usernames>();

  useEffect(() => {
    if (!jwtIsPending && jwt) {
      connect()
    }
  }, [matchId, jwtIsPending])

    const connect = () => {
        const socket = new SockJS(`https://${websocketUrl}/match`);
        // websocketUrl should really be http://localhost:8090 or http://127.0.0.1:8090 and '/match' is to be added by frontend here

        const client = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                Authorization: `Bearer ${jwt}`
            },
            debug: (str: string) => console.log(str),
            onConnect: () => {

                // Subscribe to your specific user queue for status updates
                client.subscribe(`/topic/match/${matchId}/events`, (message: IMessage) => {
                    const body: MatchEventMessage = JSON.parse(message.body);
                    console.log("Got match event message: " + body.message);
                    if (body.eventType == 'MATCH_ENDED') {
                        // if the game has ended in checkmate, stalemate, or is a draw
                        setIsGameOver(true);
                        setGameOverMessage(body.message);
                    }
                    if (body.eventType == 'TIME_EXPIRED') {
                        // if the game has ended due to time
                        setIsGameOver(true);
                        setGameOverMessage(body.message);
                    }
                    if (body.eventType == 'PLAYER_RESIGNED') {
                        setIsGameOver(true);
                        setGameOverMessage(body.message);
                    }
                    if (body.eventType == 'DRAW_ACCEPTED') {
                        // both players agree to draw
                        setIsGameOver(true);
                        setGameOverMessage(body.message);
                    }
                    if (body.eventType == 'DRAW_DECLINED') {
                        setDrawOfferState('noOffer');
                    }
                });

                client.subscribe(`/topic/match/${matchId}/time`, (message: IMessage) => {
                    const timeUpdate: TimeUpdateMessage = JSON.parse(message.body);
                    const { whitePlayerTime, blackPlayerTime } = timeUpdate;
                    setClockState({ whitePlayerTime, blackPlayerTime });
                });

                // TODO figure out if unneeded as it never seems to receive messages
                client.subscribe(`/user/queue/match/${matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    const whitePlayerName = match.whitePlayer?.displayName;
                    const blackPlayerName = match.blackPlayer?.displayName;
                    console.log(`Got initial match state message. Players are: ${whitePlayerName} and ${blackPlayerName}`);
                    setBoardState(fenToBoard(match.fen)); // should be the starting position
                    if(whitePlayerName && blackPlayerName) {
                        setUsernames({whitePlayerName, blackPlayerName});
                    }
                    setIsPlayerWhite(match.whitePlayer?.playerId === playerId);
                });

                // Subscribe to the match state. This will update our board
                client.subscribe(`/topic/match/${matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    console.log(`Is checkmate? ${match.isCheckmate}. Is stalemate? ${match.isStalemate}. Is draw? ${match.isDraw}`);
                    const newBoard = fenToBoard(match.fen)
                    const playerWithTurn = newBoard.sideToMove === 'WHITE' ? match.whitePlayer : match.blackPlayer;
                    if (playerWithTurn?.playerId === playerId) {
                        setIsPlayersTurn(true)
                    }
                    setBoardState(newBoard);
                    setIsCheck(match.isCheck);
                    setLastMove(match.lastMove && {from: algebraicToSquare(match.lastMove.from), to: algebraicToSquare(match.lastMove.to), promotion: match.lastMove.promotion});
                    if (match.lastMove) {
                        const move = { from: algebraicToSquare(match.lastMove.from), to: algebraicToSquare(match.lastMove.to), promotion: match.lastMove.promotion };
                        setPieceOrigins(prev => {
                            const originOfSquareMovedFrom = prev.get(move.from);
                            if (originOfSquareMovedFrom === undefined || originOfSquareMovedFrom === null) {
                                // if pieceOrigins has already been updated with a move, the square we're moving from will have no origin (be undefined)
                                // therefore we should leave pieceOrigins as is.
                                return prev;
                            }
                            const newPieceOrigins = new Map(prev);
                            const origin = prev.get(move.from) ?? move.from;
                            const whiteCastledKingside = origin == 60 && move.to == 62;
                            const whiteCastledQueenside = origin == 60 && move.to == 58;
                            const blackCastledKingside = origin == 4 && move.to == 6;
                            const blackCastledQueenside = origin == 4 && move.to == 2;
                            if (whiteCastledKingside) {
                                newPieceOrigins.set(61, 63);
                                newPieceOrigins.delete(63);
                            }
                            if (whiteCastledQueenside) {
                                newPieceOrigins.set(59, 56);
                                newPieceOrigins.delete(56);
                            }
                            if (blackCastledKingside) {
                                newPieceOrigins.set(5, 7);
                                newPieceOrigins.delete(7);
                            }
                            if (blackCastledQueenside) {
                                newPieceOrigins.set(3, 0);
                                newPieceOrigins.delete(0);
                            }
                            newPieceOrigins.set(move.to, origin);
                            newPieceOrigins.delete(move.from);
                            return newPieceOrigins;
                        });
                    }
                });

                client.subscribe(`/topic/match/${matchId}/draw-offer`, (message: IMessage) => {
                    const drawOfferMessage: DrawOfferMessage = JSON.parse(message.body);
                    console.log("Got draw offer message from: " + drawOfferMessage.offeredBy);
                    if(drawOfferMessage.offeredBy != playerId) {
                        setDrawOfferState('receivedOffer');
                    }
                });

                joinGame();
            }
        });

        client.activate();
        stompClient.current = client;
    }

  const joinGame = () => {
    if (stompClient.current?.connected) {
      // const randomAnimationType: Preferences['animationType'] = Math.random() > 0.5 ? 'FUN' : 'NORMAL';
      // updatePreferences({ animationType: randomAnimationType });
      // const preferencesForMatch: PlayerPreferences = {
      //     boardColour: preferences.boardColour, // boardColour can be read from the now outdated preferences object because we didn't modify it
      //     animationType: randomAnimationType
      // }
      const joinRequest: JoinMatchMessage = {
        playerId,
        matchId,
        playerName: getLocalStorage("displayName") ?? "",
        playerPreferences: preferences,
      }

      stompClient.current.publish({
        destination: `/app/match/${matchId}/join`,
        body: JSON.stringify(joinRequest),
      })
    }
  }

  const handleMove: (move: Move) => void = (move) => {
    if (stompClient.current?.connected) {
      setIsPlayersTurn(false)
      const moveRequest: MoveMessage = {
        matchId,
        playerId,
        from: squareToAlgebraic(move.from),
        to: squareToAlgebraic(move.to),
        promotion: move.promotion,
      }

            stompClient.current.publish({
                destination: `/app/match/${matchId}/move`,
                body: JSON.stringify(moveRequest)
            });
        }
    }

    const handleResign = () => {
        const resignMessage: ResignMessage = {
            matchId,
            playerId,
        }
        if (stompClient.current?.connected) {
            stompClient.current.publish({
                destination: `/app/match/${matchId}/resign`,
                body: JSON.stringify(resignMessage)
            })
        }
    }

    const offerDraw = () => {
        if (stompClient.current?.connected) {
            setDrawOfferState('waitingForResponse');
            const drawOfferMessage: DrawOfferMessage = {
                matchId,
                offeredBy: playerId
            }
            stompClient.current.publish({
                destination: `/app/match/${matchId}/draw/offer`,
                body: JSON.stringify(drawOfferMessage)
            })
        }
    }

    const respondToDraw = (accepted: boolean) => {
        if (stompClient.current?.connected) {
            setDrawOfferState('noOffer');
            const drawResponseMessage: DrawResponseMessage = {
                matchId,
                playerId,
                accepted
            }
            stompClient.current.publish({
                destination: `/app/match/${matchId}/draw/respond`,
                body: JSON.stringify(drawResponseMessage)
            })
        }
    }

  return (
    <Layout>
      <Dialog open={resignConfirmationDialog}>
      <div className="flex w-full justify-center">
        <div className="w-3/4">
          {isGameOver && gameOverMessage && <p className='mb-3 outline-solid outline-2 rounded-md p-3'>{gameOverMessage}</p>}
          <ChessBoard
            isPlayersTurn={isPlayersTurn}
            isGameOver={isGameOver}
            isCheck={isCheck}
            lastMove={lastMove}
            boardState={boardState}
            clockState={clockState}
            moveCallback={handleMove}
            whitePerspective={isPlayerWhite}
            pieceOrigins={pieceOrigins}
            usernames={usernames}
          >
                            {!isGameOver && <div className="flex items-center gap-2">
                            <Button onClick={() => setResignConfirmationDialog(true)} variant="outline">Resign</Button>
                            <Button onClick={drawOfferState == 'noOffer' ? offerDraw : () => setDrawConfirmationDialog(true)}
                                    variant={drawOfferState == 'receivedOffer' ? "default" : "outline"}
                                    disabled={drawOfferState == 'waitingForResponse'}>
                                {drawOfferState == 'noOffer' ? "Offer draw" :
                                    (drawOfferState == 'waitingForResponse' ? "Draw offered" : "Accept draw?")}
                            </Button>
                        </div>}
                        </ChessBoard>
        </div>
      </div>
      {isGameOver && <BackArrowLink />}
      <DialogContent showCloseButton={false}>
                    <DialogHeader>
                        <DialogDescription>
                        <p>Resign this game?</p>
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button onClick={() => {handleResign(); setResignConfirmationDialog(false);}} variant="destructive">Resign</Button>
                        <DialogClose asChild>
                            <Button onClick={() => setResignConfirmationDialog(false)} variant="outline">Cancel</Button>
                        </DialogClose>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
      <Dialog open={isGameOver && !surveySubmitted}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{gameOverMessage}</DialogTitle>
            <DialogDescription></DialogDescription>
          </DialogHeader>
          <p>On a scale of 1 to 5, how much did you enjoy your chess game?</p>
          <div className="flex justify-center gap-1 pb-2">
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                onClick={() => setSurveyScore(n)}
                className="focus:outline-none"
                aria-label={`Rate ${n} out of 5`}
              >
                <Star
                  className={`h-8 w-8 transition-colors ${
                    n <= surveyScore
                      ? "fill-yellow-400 text-yellow-400"
                      : "text-muted-foreground"
                  }`}
                />
              </button>
            ))}
          </div>
          <Textarea
            placeholder="Any comments? (optional)"
            value={surveyComment}
            onChange={(e) => setSurveyComment(e.target.value)}
            className="resize-none"
            rows={3}
          />
          <DialogFooter>
            <Button
              onClick={handleSurveySubmit}
              disabled={surveyScore === 0 || surveyIsPending}
            >
              {surveyIsPending ? "Submitting..." : "Submit"}
            </Button>
            <Button variant="ghost" onClick={() => setSurveySubmitted(true)}>
              Skip
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={drawConfirmationDialog}>
                <DialogContent showCloseButton={false}>
                    <DialogHeader>
                        <DialogDescription>
                        <p>Draw offered by opponent</p>
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button onClick={() => { respondToDraw(true); setDrawConfirmationDialog(false);}}>Accept draw</Button>
                        <DialogClose asChild>
                            <Button onClick={() => { respondToDraw(false); setDrawConfirmationDialog(false);}}>Reject draw</Button>
                        </DialogClose>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
    </Layout>
  )
}

export default Match
