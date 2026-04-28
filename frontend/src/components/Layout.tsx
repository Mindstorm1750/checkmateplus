import type { ReactNode } from "react"
import { Link, useNavigate } from "react-router"
import { Button } from "@/components/ui/button.tsx"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import { useJwt } from "@/lib/JwtProvider"
import { Settings, User } from "lucide-react"

export default function Layout({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const { isGuest, signOut } = useJwt()

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold lg:text-3xl">
          <Link to="/">Checkmate+</Link>
        </h1>
        <div className="flex items-center gap-2">
          {isGuest ? (
            <>
              <Button onClick={() => navigate("/login")}>Login</Button>
              <Button onClick={() => navigate("/register")}>Register</Button>
            </>
          ) : (
            <>
              <Button className="p-3" onClick={() => navigate("/profile")}>
                <div className="flex items-center justify-between gap-2">
                  <User />
                  {getLocalStorage("displayName")}
                </div>
              </Button>
              <Button
                onClick={() => {
                  signOut()
                  navigate("/")
                  window.location.reload()
                }}
              >
                Sign out
              </Button>
            </>
          )}
          <Button variant="outline" onClick={() => navigate("/settings")}>
            <Settings className="size-5" />
          </Button>
        </div>
      </div>

      <hr className="mt-4 mb-8" />
      {children}
    </div>
  )
}
