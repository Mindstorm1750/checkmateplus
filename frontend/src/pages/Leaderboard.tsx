import { useLeaderboard } from "@/hooks/useLeaderboard"
import { Spinner } from "@/components/ui/spinner"
import Layout from "@/components/Layout"
import { BackArrowLink } from "@/components/ui/back-arrow-link.tsx"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table.tsx"
import { useNavigate } from "react-router"

export default function Leaderboard() {
  const { data, isLoading, isError } = useLeaderboard(25)
  const navigate = useNavigate()

  return (
    <Layout>
      <h1 className="mb-4 text-2xl font-bold">Leaderboard</h1>
      {isLoading && <Spinner />}
      {isError && (
        <p className="text-destructive">Failed to load leaderboard.</p>
      )}
      {data && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>#</TableHead>
              <TableHead>Player</TableHead>
              <TableHead>Rating</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((entry) => (
              <TableRow key={entry.playerId}>
                <TableCell>{entry.rank}</TableCell>
                <TableCell
                  className="cursor-pointer hover:underline"
                  onClick={() => navigate(`/profile/${entry.playerId}`)}
                >
                  {entry.displayName}
                </TableCell>
                <TableCell>{entry.skillRating}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      <BackArrowLink />
    </Layout>
  )
}
