import { useLeaderboard } from '@/hooks/useLeaderboard'
import { Spinner } from '@/components/ui/spinner'
import Layout from '@/components/Layout'

export default function Leaderboard() {
    const { data, isLoading, isError } = useLeaderboard(25)

    return (
        <Layout>
            <h1 className="text-2xl font-bold mb-4">Leaderboard</h1>
            {isLoading && <Spinner />}
            {isError && <p className="text-destructive">Failed to load leaderboard.</p>}
            {data && (
                <table className="w-full text-sm">
                    <thead>
                        <tr className="text-left text-muted-foreground border-b">
                            <th className="pb-2 w-12">#</th>
                            <th className="pb-2">Player</th>
                            <th className="pb-2 text-right">Rating</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.map(entry => (
                            <tr key={entry.playerId} className="border-b last:border-0">
                                <td className="py-2 text-muted-foreground">{entry.rank}</td>
                                <td className="py-2 font-medium">{entry.displayName}</td>
                                <td className="py-2 text-right tabular-nums">{entry.skillRating}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </Layout>
    )
}
