/**
 * Le proposte dell'AI.
 *
 * Compaiono solo su richiesta, non a ogni messaggio ricevuto: ogni richiesta è
 * una chiamata a un modello esterno, e farla partire da sola significherebbe
 * bruciare quota e mostrare un'attesa che nessuno ha chiesto.
 *
 * Al clic la proposta finisce NELLA CASELLA, non spedita: resta modificabile,
 * e l'invio resta una decisione dell'utente. È anche il motivo per cui nulla
 * di tutto questo viene salvato sul server.
 */
export default function SuggestionBar({
  proposte,
  modello,
  inCorso,
  errore,
  onChiedi,
  onScegli,
  disabilitato,
}) {
  return (
    <div className="suggerimenti">
      <button
        type="button"
        className="chip azione"
        onClick={onChiedi}
        disabled={inCorso || disabilitato}
      >
        {inCorso ? 'Ci penso…' : '✨ Suggerisci una risposta'}
      </button>

      {proposte.map((proposta, i) => (
        <button
          key={i}
          type="button"
          className="chip"
          onClick={() => onScegli(proposta)}
          title="Mettilo nella casella di testo"
        >
          {proposta}
        </button>
      ))}

      {errore && <span className="nota-ai">⚠ {errore}</span>}

      {proposte.length > 0 && !errore && (
        <span className="nota-ai">
          Proposte da {modello}. Non vengono salvate: scegline una per metterla
          nella casella, poi modificala come vuoi.
        </span>
      )}
    </div>
  )
}
