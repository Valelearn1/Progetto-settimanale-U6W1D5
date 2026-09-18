import { useEffect, useRef } from 'react'

export default function Composer({ testo, setTesto, onInvia, disabilitato }) {
  const area = useRef(null)

  // L'area cresce col testo invece di mostrare una barra di scorrimento,
  // fino al tetto fissato in CSS (max-height).
  useEffect(() => {
    const el = area.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [testo])

  function suTasto(evento) {
    // Invio manda, Shift+Invio va a capo: la convenzione di tutte le chat.
    if (evento.key === 'Enter' && !evento.shiftKey) {
      evento.preventDefault()
      onInvia()
    }
  }

  return (
    <div className="composer-riga">
      <textarea
        ref={area}
        rows={1}
        value={testo}
        placeholder="Scrivi un messaggio…"
        onChange={(e) => setTesto(e.target.value)}
        onKeyDown={suTasto}
        disabled={disabilitato}
      />
      <button
        type="button"
        className="bottone-principale"
        style={{ padding: '11px 18px' }}
        onClick={onInvia}
        disabled={disabilitato || !testo.trim()}
      >
        Invia
      </button>
    </div>
  )
}
