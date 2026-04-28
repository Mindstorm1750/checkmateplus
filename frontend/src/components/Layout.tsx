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
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-xl font-bold md:text-2xl lg:text-3xl">
          <Link to="/">Checkmate+</Link>
        </h1>
        <div className="flex min-w-0 items-center gap-2">
          {isGuest ? (
            <>
              <Button onClick={() => navigate("/login")}>Login</Button>
              <Button onClick={() => navigate("/register")}>Register</Button>
            </>
          ) : (
            <>
              <Button
                className="min-w-10 shrink p-3"
                onClick={() => navigate("/profile")}
              >
                <div className="min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <User className="shrink-0" />
                    <span className="truncate">
                      {getLocalStorage("displayName")}
                    </span>
                  </div>
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
