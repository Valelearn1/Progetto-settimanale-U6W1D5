import { useTheme } from '../context/ThemeContext'

const ETICHETTE = {
  sistema: { icona: '◐', testo: 'Tema: sistema' },
  light: { icona: '☀', testo: 'Tema: chiaro' },
  dark: { icona: '☾', testo: 'Tema: scuro' },
}

/**
 * Un solo pulsante che cicla fra i tre stati, invece di due pulsanti o di un
 * interruttore a due posizioni: un interruttore non saprebbe rappresentare
 * "segui il sistema", che è il default.
 *
 * Il title dice sempre lo stato corrente E il prossimo, così non serve provare
 * per capire cosa fa.
 */
export default function ThemeToggle({ esteso = false }) {
  const { tema, cambia } = useTheme()
  const { icona, testo } = ETICHETTE[tema]

  const prossimo = tema === 'sistema' ? 'chiaro' : tema === 'light' ? 'scuro' : 'sistema'

  return (
    <button
      type="button"
      className="bottone-secondario tema-toggle"
      onClick={cambia}
      title={`${testo} — clicca per passare a: ${prossimo}`}
      aria-label={`${testo}. Clicca per passare a ${prossimo}`}
    >
      <span aria-hidden="true">{icona}</span>
      {esteso && <span>{testo}</span>}
    </button>
  )
}
