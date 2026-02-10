import Layout from "@/components/Layout";
import { useParams } from "react-router";

function Play() {
    const { colour } = useParams();
    return (
        <Layout>
            <h1>{`You chose ${colour}`}</h1>;
        </Layout>
    )
}

export default Play