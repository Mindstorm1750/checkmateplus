import { ArrowLeft } from "lucide-react";
import { Link } from "react-router";

export const BackArrowLink = () => {
    return (
        <Link to='/'>
            <ArrowLeft className='mt-4' />
        </Link>
    )
}