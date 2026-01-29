import { useParams } from "react-router";

function Play() {
    const { colour } = useParams();
    return <h1>{`You chose ${colour}`}</h1>;
}

export default Play