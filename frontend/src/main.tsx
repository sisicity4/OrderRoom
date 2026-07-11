import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {BrowserRouter, Routes, Route }from 'react-router-dom'
import CreatePage from './pages/CreatePage'
import JoinPage from './pages/JoinPage'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
    <Routes>
      <Route path="/" element={<App />} />
      <Route path="/create" element={<CreatePage />} />
      <Route path="/join" element={<JoinPage />} />
    </Routes>
    </BrowserRouter>
  </StrictMode>,
)
