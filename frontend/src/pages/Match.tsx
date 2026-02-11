import Layout from "@/components/Layout";
import { useParams } from "react-router";

function Match() {
    const { matchId } = useParams();
    return (
        <Layout>
            <h1>{`MatchId ${matchId}`}</h1>;
        </Layout>
    )
}

export default Match