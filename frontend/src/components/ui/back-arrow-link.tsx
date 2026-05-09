import { ArrowLeft } from "lucide-react"
import { useNavigate } from "react-router"

export const BackArrowLink = () => {
  const navigate = useNavigate()
  return (
    <ArrowLeft
      className="mt-4 cursor-pointer hover:[animation:wiggle_1s_ease-in-out_0.2s_1_forwards]"
      onClick={() => navigate(-1)}
    />
  )
}
