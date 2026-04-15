import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './pages/App.tsx'
import { ThemeProvider } from './components/theme-provider.tsx'
import { BrowserRouter, Route, Routes } from 'react-router'
import Play from './pages/Play.tsx'
import { PreferencesProvider } from './lib/PreferencesProvider.tsx'
import Settings from './pages/Settings.tsx'
import Match from './pages/Match.tsx'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import Leaderboard from './pages/Leaderboard.tsx'
import { JwtProvider } from './lib/JwtProvider.tsx'
import Login from "@/pages/Login.tsx";

const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <JwtProvider>
        <PreferencesProvider> {/** For chessboard related preferences (square colours, animations) */}
          <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme"> {/** Website theme */}
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<App />} />
                <Route path='/play/:colour' element={<Play />} />
                <Route path='/settings' element={<Settings />} />
                <Route path='/match' element={<Match />} />
                <Route path='/leaderboard' element={<Leaderboard />} />
                <Route path='/login' element={<Login />} />
              </Routes>
            </BrowserRouter>
          </ThemeProvider>
        </PreferencesProvider>
      </JwtProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </StrictMode>
)
