import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

const CHIAVE = 'filorosso.tema'

const ThemeContext = createContext(null)

/**
 * Tre stati, non due: chiaro, scuro, oppure "come il sistema".
 *
 * Il terzo non è un di più: è il default, ed è quello che rispetta la scelta
 * che l'utente ha già fatto a livello di sistema operativo. Chi tiene il Mac in
 * automatico (chiaro di giorno, scuro di sera) si aspetta che l'applicazione lo
 * segua senza doverglielo dire.
 *
 * L'attributo data-theme viene messo sull'elemento <html> solo per le scelte
 * esplicite: quando è "sistema" l'attributo sparisce, e a decidere resta la
 * media query prefers-color-scheme nel CSS.
 */
export function ThemeProvider({ children }) {
  const [tema, setTema] = useState(() => localStorage.getItem(CHIAVE) ?? 'sistema')

  useEffect(() => {
    const radice = document.documentElement
    if (tema === 'sistema') {
      radice.removeAttribute('data-theme')
      localStorage.removeItem(CHIAVE)
    } else {
      radice.setAttribute('data-theme', tema)
      localStorage.setItem(CHIAVE, tema)
    }
  }, [tema])

  /** Quale tema si sta effettivamente vedendo adesso. */
  const temaEffettivo = useMemo(() => {
    if (tema !== 'sistema') return tema
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
  }, [tema])

  // Ciclo: sistema -> chiaro -> scuro -> sistema
  const cambia = useCallback(() => {
    setTema((precedente) =>
      precedente === 'sistema' ? 'light' : precedente === 'light' ? 'dark' : 'sistema',
    )
  }, [])

  const valore = useMemo(
    () => ({ tema, temaEffettivo, cambia, setTema }),
    [tema, temaEffettivo, cambia],
  )

  return <ThemeContext.Provider value={valore}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const contesto = useContext(ThemeContext)
  if (!contesto) {
    throw new Error('useTheme va usato dentro <ThemeProvider>')
  }
  return contesto
}
