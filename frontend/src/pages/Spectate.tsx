import Layout from "@/components/Layout"
import {BackArrowLink} from "@/components/ui/back-arrow-link"
import {Button} from "@/components/ui/button"
import {Card, CardContent} from "@/components/ui/card"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import MiniChessBoard from "@/components/ui/MiniChessBoard"
import {Spinner} from "@/components/ui/spinner"
import {useLiveMatches} from "@/hooks/useLiveMatches"
import {usePlayerStats} from "@/hooks/usePlayerStats"
import {fenToBoard, pieceToSvg, STARTING_FEN} from "@/lib/chess.helpers"
import type {
    SpectateMatch,
    SpectatePlayer,
} from "@/lib/playerServiceTypes/SpectateMatch"
import type {MatchType} from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/MatchType"
import {TimeControlToStringMap} from "@/lib/stompMessageTypes/ChessLibrary/PlayerModels/TimeControl"
import {Bot} from "lucide-react"
import {useState} from "react"
import {useNavigate} from "react-router"

const matchTypeLabel: Record<MatchType, string> = {
    CASUAL_PVP: "Casual",
    COMPETITIVE_PVP: "Ranked",
    PVE: "vs Bot",
}

function formatTime(seconds: number): string {
    const safe = Math.max(0, Math.floor(seconds))
    const mins = Math.floor(safe / 60)
    const secs = safe % 60
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`
}

function playerLabel(player?: SpectatePlayer): string {
    if (!player) return "—"
    if (player.type === "ANONYMOUS") return player.displayName || "Guest"
    return player.displayName
}

export default function Spectate() {
    const {
        data,
        isLoading,
        isError,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useLiveMatches()
    const [selected, setSelected] = useState<SpectateMatch | null>(null)

    const matches = data?.pages.flatMap((p) => p.matches) ?? []

    return (
        <Layout>
            <h1 className="mb-4 text-2xl font-bold">Live Matches</h1>
            {isLoading && <Spinner/>}
            {isError && (
                <p className="text-destructive">Failed to load live matches.</p>
            )}
            {!isLoading && matches.length === 0 && (
                <p className="text-muted-foreground">No live matches right now.</p>
            )}
            {matches.length > 0 && (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {matches.map((match) => (
                        <MatchCard
                            key={match.matchId}
                            match={match}
                            onClick={() => setSelected(match)}
                        />
                    ))}
                </div>
            )}
            {hasNextPage && (
                <div className="mt-4 flex justify-center">
                    <Button
                        variant="outline"
                        onClick={() => fetchNextPage()}
                        disabled={isFetchingNextPage}
                    >
                        {isFetchingNextPage ? "Loading..." : "Load more"}
                    </Button>
                </div>
            )}
            <BackArrowLink/>
            <Dialog
                open={selected !== null}
                onOpenChange={(open) => !open && setSelected(null)}
            >
                <DialogContent className="flex max-h-[90dvh] flex-col overflow-y-auto sm:max-w-4xl">
                    {selected && <MatchDetails match={selected}/>}
                </DialogContent>
            </Dialog>
        </Layout>
    )
}

function MatchCard({
                       match,
                       onClick,
                   }: {
    match: SpectateMatch
    onClick: () => void
}) {
    const board = fenToBoard(match.lastFen ?? STARTING_FEN)
    const tcLabel = match.timeControl
        ? (TimeControlToStringMap.get(match.timeControl) ?? match.timeControl)
        : "—"
    const mtLabel = match.matchType ? matchTypeLabel[match.matchType] : "—"

    return (
        <Card
            onClick={onClick}
            className="cursor-pointer transition-colors hover:border-primary"
        >
            <CardContent className="flex flex-col gap-3 p-3">
                <PlayerRow player={match.blackPlayer} time={match.blackTimeSeconds}/>
                <MiniChessBoard boardState={board}/>
                <PlayerRow player={match.whitePlayer} time={match.whiteTimeSeconds}/>
                <div className="flex justify-between text-xs text-muted-foreground">
                    <span>{mtLabel}</span>
                    <span>{tcLabel}</span>
                    <span>Move {match.moveNumber}</span>
                </div>
            </CardContent>
        </Card>
    )
}

function PlayerRow({
                       player,
                       time,
                   }: {
    player?: SpectatePlayer
    time: number
}) {
    return (
        <div className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
                <PlayerTypeIcon type={player?.type}/>
                <span className="font-medium">{playerLabel(player)}</span>
            </div>
            <span className="rounded bg-muted px-2 py-0.5 font-mono text-xs tabular-nums">
        {formatTime(time)}
      </span>
        </div>
    )
}

function PlayerTypeIcon({type}: { type?: SpectatePlayer["type"] }) {
    if (type === "BOT") return <Bot className="h-4 w-4"/>
    if (type === "ANONYMOUS")
        return (
            <img
                src={pieceToSvg({pieceType: "PAWN", colour: "WHITE"})}
                alt="Guest"
                className="h-4 w-4"
                draggable="false"
            />
        )
    if (type === "REGISTERED")
        return (
            <img
                src={pieceToSvg({pieceType: "KING", colour: "WHITE"})}
                alt="Registered"
                className="h-4 w-4"
                draggable="false"
            />
        )
    return null
}

function MatchDetails({match}: { match: SpectateMatch }) {
    const navigate = useNavigate()
    const board = fenToBoard(match.lastFen ?? STARTING_FEN)
    const tcLabel = match.timeControl
        ? (TimeControlToStringMap.get(match.timeControl) ?? match.timeControl)
        : "—"
    const mtLabel = match.matchType ? matchTypeLabel[match.matchType] : "—"
    const canWatchLive = !!match.websocketUrl && match.status === "ACTIVE"

    return (
        <>
            <DialogHeader>
                <DialogTitle>
                    {playerLabel(match.whitePlayer)} vs {playerLabel(match.blackPlayer)}
                </DialogTitle>
            </DialogHeader>
            {canWatchLive && (
                <div className="flex justify-end">
                    <Button
                        onClick={() =>
                            navigate(`/spectate/${match.matchId}`, {state: match})
                        }
                    >
                        Watch live
                    </Button>
                </div>
            )}
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-[1fr_auto_1fr]">
                <SpectatePlayerPanel
                    player={match.whitePlayer}
                    colour="WHITE"
                    time={match.whiteTimeSeconds}
                    botModel={
                        match.botConfiguration && match.whitePlayer?.type === "BOT"
                            ? match.botConfiguration.botModelType
                            : undefined
                    }
                />
                <div className="w-full sm:w-72 order-last sm:order-none sm:col-span-1 col-span-2">
                    <MiniChessBoard boardState={board}/>
                </div>
                <SpectatePlayerPanel
                    player={match.blackPlayer}
                    colour="BLACK"
                    time={match.blackTimeSeconds}
                    botModel={
                        match.botConfiguration && match.blackPlayer?.type === "BOT"
                            ? match.botConfiguration.botModelType
                            : undefined
                    }
                    alignRight
                />
            </div>
            <div className="grid grid-cols-2 gap-2 border-t pt-3 text-sm sm:grid-cols-4">
                <Detail label="Status" value={match.status}/>
                <Detail label="Move" value={match.moveNumber.toString()}/>
                <Detail label="Type" value={mtLabel}/>
                <Detail label="Time" value={tcLabel}/>
                {match.reason && (
                    <Detail label="Reason" value={match.reason} className="col-span-2"/>
                )}
            </div>
        </>
    )
}

function SpectatePlayerPanel({
                                 player,
                                 colour,
                                 time,
                                 botModel,
                                 alignRight = false,
                             }: {
    player?: SpectatePlayer
    colour: "WHITE" | "BLACK"
    time: number
    botModel?: string
    alignRight?: boolean
}) {
    const align = alignRight ? "items-end text-right" : "items-start text-left"
    return (
        <div className={`flex flex-col gap-2 ${align}`}>
            <div className="flex items-center gap-2">
                <PlayerTypeIcon type={player?.type}/>
                <span className="font-semibold">{playerLabel(player)}</span>
            </div>
            <span className="text-xs text-muted-foreground">
        Plays as {colour === "WHITE" ? "White" : "Black"}
      </span>
            <span className="rounded bg-muted px-2 py-0.5 font-mono text-sm tabular-nums">
        {formatTime(time)}
      </span>
            {botModel && (
                <span className="text-xs text-muted-foreground">Model {botModel}</span>
            )}
            {player?.type === "REGISTERED" && (
                <RegisteredPlayerStats playerId={player.playerId}/>
            )}
        </div>
    )
}

function RegisteredPlayerStats({playerId}: { playerId: string }) {
    const {data, isPending, isError} = usePlayerStats(playerId)

    if (isPending) return <Spinner/>
    if (isError || !data)
        return (
            <span className="text-xs text-muted-foreground">Stats unavailable</span>
        )

    return (
        <div className="grid grid-cols-2 gap-x-3 gap-y-1 pt-1 text-xs tabular-nums">
            <Stat label="Rating" value={data.skillRating ?? "—"}/>
            <Stat label="Games" value={data.totalGames}/>
            <Stat label="Win rate" value={`${data.winRate}%`}/>
            <Stat label="Wins" value={data.wins}/>
            <Stat label="Losses" value={data.losses}/>
            <Stat label="Draws" value={data.draws}/>
            <Stat label="Streak" value={data.currentStreak}/>
            <Stat label="Best streak" value={data.longestWinStreak}/>
            {data.favouriteTimeControl && (
                <Stat
                    label="Fav TC"
                    value={
                        TimeControlToStringMap.get(data.favouriteTimeControl) ??
                        data.favouriteTimeControl
                    }
                />
            )}
        </div>
    )
}

function Stat({label, value}: { label: string; value: string | number }) {
    return (
        <>
            <span className="text-muted-foreground">{label}</span>
            <span className="font-medium">{value}</span>
        </>
    )
}

function Detail({
                    label,
                    value,
                    className,
                }: {
    label: string
    value: string
    className?: string
}) {
    return (
        <div className={className}>
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="font-medium">{value}</p>
        </div>
    )
}
