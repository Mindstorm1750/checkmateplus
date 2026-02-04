import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './pages/App.tsx'
import { ThemeProvider } from './components/theme-provider.tsx'
import { BrowserRouter, Route, Routes } from 'react-router'
import MatchmakerTestApp from './pages/MatchmakerTestApp.tsx'
import Play from './pages/Play.tsx'
import Test from './pages/Test.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path='/test' element={<MatchmakerTestApp />} />
          <Route path='/play/:colour' element={<Play />} />
          <Route path='/testBoard' element={<Test />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>
)
