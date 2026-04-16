import { useLeaderboard } from "@/hooks/useLeaderboard"
import { Spinner } from "@/components/ui/spinner"
import Layout from "@/components/Layout"
import { BackArrowLink } from "@/components/ui/back-arrow-link.tsx"

export default function Leaderboard() {
  const { data, isLoading, isError } = useLeaderboard(25)

  return (
    <Layout>
      <h1 className="mb-4 text-2xl font-bold">Leaderboard</h1>
      {isLoading && <Spinner />}
      {isError && (
        <p className="text-destructive">Failed to load leaderboard.</p>
      )}
      {data && (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-muted-foreground">
              <th className="w-12 pb-2">#</th>
              <th className="pb-2">Player</th>
              <th className="pb-2 text-right">Rating</th>
            </tr>
          </thead>
          <tbody>
            {data.map((entry) => (
              <tr key={entry.playerId} className="border-b last:border-0">
                <td className="py-2 text-muted-foreground">{entry.rank}</td>
                <td className="py-2 font-medium">{entry.displayName}</td>
                <td className="py-2 text-right tabular-nums">
                  {entry.skillRating}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <BackArrowLink />
    </Layout>
  )
}
