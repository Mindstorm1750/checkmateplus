import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './pages/App.tsx'
import { ThemeProvider } from './components/theme-provider.tsx'
import { BrowserRouter, Route, Routes } from 'react-router'
import MatchmakerTestApp from './pages/MatchmakerTestApp.tsx'
import Play from './pages/Play.tsx'
import Test from './pages/Test.tsx'
import { PreferencesProvider } from './lib/PreferencesContext.tsx'
import Settings from './pages/Settings.tsx'
import Match from './pages/Match.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <PreferencesProvider> {/** For chessboard related preferences (square colours, animations) */}
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme"> {/** Website theme */}
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path='/test' element={<MatchmakerTestApp />} />
          <Route path='/play/:colour' element={<Play />} />
          <Route path='/testBoard' element={<Test />} />
          <Route path='/settings' element={<Settings />} />
          <Route path='/match/:matchId' element={<Match />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
    </PreferencesProvider>
  </StrictMode>
)
