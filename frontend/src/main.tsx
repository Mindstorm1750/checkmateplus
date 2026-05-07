import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import "./index.css"
import App from "./pages/App.tsx"
import { ThemeProvider } from "./components/theme-provider.tsx"
import { BrowserRouter, Route, Routes } from "react-router"
import { PreferencesProvider } from "./lib/PreferencesProvider.tsx"
import Match from "./pages/Match.tsx"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { ReactQueryDevtools } from "@tanstack/react-query-devtools"
import Leaderboard from "./pages/Leaderboard.tsx"
import Spectate from "./pages/Spectate.tsx"
import SpectateMatch from "./pages/SpectateMatch.tsx"
import { JwtProvider } from "./lib/JwtProvider.tsx"
import Login from "@/pages/Login.tsx"
import Register from "@/pages/Register.tsx"
import ConfirmTotp from "@/pages/ConfirmTotp.tsx"
import Profile from "@/pages/Profile.tsx"

const queryClient = new QueryClient()

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <JwtProvider>
        <PreferencesProvider>
          {/** For chessboard related preferences (square colours, animations) */}
          <ThemeProvider defaultTheme="system" storageKey="vite-ui-theme">
            {/** Website theme */}
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<App />} />
                <Route path="/match" element={<Match />} />
                <Route path="/leaderboard" element={<Leaderboard />} />
                <Route path="/spectate" element={<Spectate />} />
                <Route path="/spectate/:matchId" element={<SpectateMatch />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/confirmTOTP" element={<ConfirmTotp />} />
                <Route path="/profile/:playerId" element={<Profile />} />
              </Routes>
            </BrowserRouter>
          </ThemeProvider>
        </PreferencesProvider>
      </JwtProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </StrictMode>
)
