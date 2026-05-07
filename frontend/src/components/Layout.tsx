import { useState, type ReactNode } from "react"
import { Link, useNavigate } from "react-router"
import { Button } from "@/components/ui/button.tsx"
import { getLocalStorage } from "@/lib/local-storage.helpers.ts"
import { useJwt } from "@/lib/JwtProvider"
import { Settings, User } from "lucide-react"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog.tsx"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group.tsx"
import { type Preferences, usePreferences } from "@/lib/PreferencesProvider.tsx"
import { BoardPreview } from "@/components/ui/board-preview.tsx"
import { type Theme, useTheme } from "@/components/theme-provider.tsx"

export default function Layout({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const { preferences, updatePreferences } = usePreferences()
  const { theme, setTheme } = useTheme()
  const { isGuest, signOut } = useJwt()
  const [signingOut, setSigningOut] = useState(false)
  const ourPlayerId = getLocalStorage("playerId")

  const handleSignOut = async () => {
    if (signingOut) return
    setSigningOut(true)
    try {
      await signOut()
      navigate("/")
    } finally {
      setSigningOut(false)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="flex items-center justify-between gap-2">
        <h1 className="font-serif text-2xl font-bold tracking-tight md:text-3xl lg:text-4xl">
          <Link to="/">
            Checkmate
            <span className="font-sans font-black text-brand-accent">+</span>
          </Link>
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
                onClick={() => navigate(`/profile/${ourPlayerId}`)}
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
              <Button onClick={handleSignOut} disabled={signingOut}>
                {signingOut ? "Signing out..." : "Sign out"}
              </Button>
            </>
          )}
          <Dialog>
            <DialogTrigger asChild>
              <Button variant="outline">
                <Settings className="size-5" />
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Settings</DialogTitle>
                <div className="flex flex-col items-start gap-2">
                  <p className="pt-2">Board colour theme:</p>
                  <ToggleGroup
                    type="single"
                    variant="outline"
                    value={preferences.boardColour}
                    onValueChange={(e) =>
                      updatePreferences({
                        boardColour: e as Preferences["boardColour"],
                      })
                    }
                  >
                    <ToggleGroupItem value="BROWN">
                      <span>Brown</span>
                      <BoardPreview theme="BROWN" />
                    </ToggleGroupItem>
                    <ToggleGroupItem value="BLUE">
                      <span>Blue</span>
                      <BoardPreview theme="BLUE" />
                    </ToggleGroupItem>
                    <ToggleGroupItem value="GREEN">
                      <span>Green</span>
                      <BoardPreview theme="GREEN" />
                    </ToggleGroupItem>
                  </ToggleGroup>

                  <p className="pt-2">Piece animations:</p>
                  <ToggleGroup
                    type="single"
                    variant="outline"
                    value={preferences.animationType}
                    onValueChange={(e) =>
                      updatePreferences({
                        animationType: e as Preferences["animationType"],
                      })
                    }
                  >
                    <ToggleGroupItem value="NORMAL">Normal</ToggleGroupItem>
                    <ToggleGroupItem value="FUN">Fun</ToggleGroupItem>
                  </ToggleGroup>
                  <p className="pt-2">Theme:</p>
                  <ToggleGroup
                    type="single"
                    variant="outline"
                    value={theme}
                    onValueChange={(e) => setTheme(e as Theme)}
                  >
                    <ToggleGroupItem value="light">Light</ToggleGroupItem>
                    <ToggleGroupItem value="dark">Dark</ToggleGroupItem>
                    <ToggleGroupItem value="system">System</ToggleGroupItem>
                  </ToggleGroup>
                </div>
              </DialogHeader>
              <DialogFooter>
                <DialogClose asChild>
                  <Button type="button">Close</Button>
                </DialogClose>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <hr className="mt-4 mb-8" />
      {children}
    </div>
  )
}
