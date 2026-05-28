import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { useNavigate } from "react-router"

type Props = {
  onContinueAsGuest: () => void
  className?: string
}

export function WelcomeGuestCard({ onContinueAsGuest, className = "" }: Props) {
  const navigate = useNavigate()
  return (
    <div className={`flex h-full flex-col gap-10 bg-secondary ${className}`}>
      <div>
        <h1 className="font-serif text-5xl leading-[1.02] tracking-tight md:text-6xl">
          <b>Welcome!</b>
        </h1>
        <p className="mt-6 max-w-sm text-sm leading-relaxed text-muted-foreground">
          Create an account to track your rating, save your match history and
          climb the leaderboard.
        </p>
      </div>
      <div className="flex flex-col gap-4">
        <Button className="min-h-12" onClick={() => navigate("/register")}>
          Create account
        </Button>
        <button
          type="button"
          onClick={onContinueAsGuest}
          className="group inline-flex cursor-pointer items-center gap-1.5 self-start text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          Continue as guest
          <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
        </button>
      </div>
    </div>
  )
}
