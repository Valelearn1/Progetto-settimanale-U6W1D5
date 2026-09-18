import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { accedi as accediApi } from '../api'

const CHIAVE = 'filorosso.sessione'

const AuthContext = createContext(null)

/**
 * La sessione vive in localStorage: ricaricando la pagina si resta dentro.
 *
 * Il token e' un JWT: il payload e' solo codificato in base64, non cifrato,
 * quindi non contiene nulla di sensibile. Quello che nessuno puo' fare senza
 * il segreto del server e' falsificarne la firma.
 */
export function AuthProvider({ children }) {
  const [sessione, setSessione] = useState(() => {
    try {
      const salvata = localStorage.getItem(CHIAVE)
      return salvata ? JSON.parse(salvata) : null
    } catch {
      return null
    }
  })

  const login = useCallback(async (email, password) => {
    const risposta = await accediApi(email, password)
    const nuova = { token: risposta.token, utente: risposta.utente }
    localStorage.setItem(CHIAVE, JSON.stringify(nuova))
    setSessione(nuova)
    return nuova
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(CHIAVE)
    setSessione(null)
  }, [])

  const valore = useMemo(
    () => ({
      token: sessione?.token ?? null,
      utente: sessione?.utente ?? null,
      autenticato: Boolean(sessione?.token),
      login,
      logout,
    }),
    [sessione, login, logout],
  )

  return <AuthContext.Provider value={valore}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const contesto = useContext(AuthContext)
  if (!contesto) {
    throw new Error('useAuth va usato dentro <AuthProvider>')
  }
  return contesto
}
