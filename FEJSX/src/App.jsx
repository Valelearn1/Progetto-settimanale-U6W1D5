import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import Chat from './pages/Chat'
import Login from './pages/Login'
import Registrazione from './pages/Registrazione'
import Statistiche from './pages/Statistiche'
import Verifica from './pages/Verifica'
import './App.css'

/** Chi non ha un token valido finisce al login. */
function Protetta({ children }) {
  const { autenticato } = useAuth()
  return autenticato ? children : <Navigate to="/login" replace />
}

/** Chi è già dentro non ha motivo di rivedere login e registrazione. */
function SoloOspiti({ children }) {
  const { autenticato } = useAuth()
  return autenticato ? <Navigate to="/chat" replace /> : children
}

function Rotte() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/chat" replace />} />

      <Route path="/login" element={<SoloOspiti><Login /></SoloOspiti>} />
      <Route
        path="/registrazione"
        element={<SoloOspiti><Registrazione /></SoloOspiti>}
      />
      {/* La verifica resta sempre raggiungibile: ci si arriva dal link della
          mail, e non è detto che il browser sia quello dove hai la sessione. */}
      <Route path="/verifica" element={<Verifica />} />

      <Route path="/chat" element={<Protetta><Chat /></Protetta>} />
      <Route path="/statistiche" element={<Protetta><Statistiche /></Protetta>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Rotte />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
