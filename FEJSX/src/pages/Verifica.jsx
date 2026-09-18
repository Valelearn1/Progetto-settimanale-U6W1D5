import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { verifica as verificaApi } from '../api'

/**
 * La pagina su cui atterra il pulsante della mail di conferma.
 *
 * L'indirizzo è `/verifica?email=…&codice=…`: se entrambi i parametri ci sono,
 * l'attivazione parte da sola e all'utente non resta niente da fare. Se manca
 * qualcosa — perché il client di posta ha spezzato il link, o perché si è
 * arrivati qui a mano — i campi restano compilabili: il codice è stampato in
 * chiaro nella mail proprio per questo.
 */
export default function Verifica() {
  const [parametri] = useSearchParams()
  const posizione = useLocation()
  const navigate = useNavigate()

  const [email, setEmail] = useState(
    parametri.get('email') ?? posizione.state?.email ?? '',
  )
  const [codice, setCodice] = useState(parametri.get('codice') ?? '')
  const [errore, setErrore] = useState(null)
  const [inCorso, setInCorso] = useState(false)

  // Evita che il tentativo automatico riparta a ogni render.
  const giaTentato = useRef(false)

  async function attiva(emailDaUsare, codiceDaUsare) {
    setErrore(null)
    setInCorso(true)
    try {
      await verificaApi(emailDaUsare.trim(), codiceDaUsare.trim())
      navigate('/login', {
        replace: true,
        state: { messaggio: 'Account attivato. Ora puoi accedere.' },
      })
    } catch (e) {
      setErrore(e.message)
    } finally {
      setInCorso(false)
    }
  }

  useEffect(() => {
    const e = parametri.get('email')
    const c = parametri.get('codice')
    if (e && c && !giaTentato.current) {
      giaTentato.current = true
      attiva(e, c)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="accesso">
      <div className="accesso-card">
        <form
          className="accesso-corpo"
          onSubmit={(evento) => {
            evento.preventDefault()
            attiva(email, codice)
          }}
        >
          <div className="marchio">
            <span className="marchio-quadrato">F</span>
            Filo Rosso
          </div>

          <div>
            <h1>Attiva il tuo account</h1>
            <p className="sottotitolo">
              Ti abbiamo mandato un codice via email. Apri il link che trovi nel
              messaggio, oppure incolla qui il codice.
            </p>
          </div>

          {errore && <div className="avviso">{errore}</div>}

          <div className="campo">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="campo">
            <label htmlFor="codice">Codice di attivazione</label>
            <input
              id="codice"
              value={codice}
              onChange={(e) => setCodice(e.target.value)}
              style={{ fontFamily: 'ui-monospace, monospace' }}
              placeholder="32 caratteri, es. Lr4KSDoCXcHnHGZ…"
              required
            />
            <span className="nota-ai">
              È la stringa lunga stampata nel riquadro grigio della mail, sotto
              al pulsante rosso. Non è un codice numerico.
            </span>
          </div>

          <button type="submit" className="bottone-principale" disabled={inCorso}>
            {inCorso ? 'Verifica in corso…' : 'Attiva account'}
          </button>

          {/* Questa pagina non deve essere un vicolo cieco: chi ha già attivato
              l'account, o non trova la mail, deve poter uscire da qui. */}
          <p className="sottotitolo">
            Non trovi la mail? Controlla nello spam.
            <br />
            Account già attivato? <Link className="link" to="/login">Accedi</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
