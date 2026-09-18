import { Suspense, lazy, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { reteContatti, statistiche, statistichePerEmail } from '../api'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import ThemeToggle from '../components/ThemeToggle'

// three.js pesa ~600 KB: il grafo viene scaricato solo quando si apre questa
// pagina, così la chat — che è l'uso quotidiano — non ne paga il peso.
const GrafoContatti = lazy(() => import('../components/network/GrafoContatti'))

const DATA = new Intl.DateTimeFormat('it-IT', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

export default function Statistiche() {
  const { token, utente } = useAuth()
  const { temaEffettivo } = useTheme()

  const [dati, setDati] = useState(null)
  const [rete, setRete] = useState([])
  const [errore, setErrore] = useState(null)
  const [esito, setEsito] = useState(null)
  const [invioInCorso, setInvioInCorso] = useState(false)

  useEffect(() => {
    statistiche(token).then(setDati).catch((e) => setErrore(e.message))
    reteContatti(token)
      // Le conversazioni aperte ma mai usate non sono fili: tenerle farebbe
      // sballare il conto dei nodi rispetto alla statistica "chat aperte".
      .then((contatti) => setRete(contatti.filter((c) => c.scambiati > 0)))
      .catch((e) => setErrore(e.message))
  }, [token])

  async function inviaReport() {
    setInvioInCorso(true)
    setEsito(null)
    setErrore(null)
    try {
      const risposta = await statistichePerEmail(token)
      setEsito(risposta.messaggio)
    } catch (e) {
      setErrore(e.message)
    } finally {
      setInvioInCorso(false)
    }
  }

  return (
    <div className="pagina">
      <div className="pagina-intestazione">
        <div className="riga-azioni">
          <Link className="bottone-secondario" to="/chat">
            ← Torna alle conversazioni
          </Link>
          <div className="marchio">
            <span className="marchio-quadrato">F</span>
            Filo Rosso
          </div>
        </div>
        <ThemeToggle esteso />
      </div>

      <h1>Le tue statistiche</h1>
      <p className="sottotitolo">
        {dati
          ? `La tua attività da quando ti sei iscritto, il ${DATA.format(new Date(dati.dal))}.`
          : 'Calcolo in corso…'}
      </p>

      {errore && <div className="avviso" style={{ marginBottom: 20 }}>{errore}</div>}
      {esito && <div className="avviso ok" style={{ marginBottom: 20 }}>{esito}</div>}

      {dati && (
        <>
          <div className="griglia-stat">
            <div className="stat">
              <div className="numero">{dati.inviati}</div>
              <div className="etichetta">Messaggi inviati</div>
            </div>
            <div className="stat">
              <div className="numero">{dati.ricevuti}</div>
              <div className="etichetta">Messaggi ricevuti</div>
            </div>
            <div className="stat">
              <div className="numero">{dati.chatAperte}</div>
              <div className="etichetta">Chat aperte</div>
            </div>
          </div>

          {rete.length > 0 && (
            <div className="grafo-riquadro">
              <div className="grafo-testata">
                <h2>I tuoi fili</h2>
                <p>
                  Sei al centro. Ogni filo rosso ti lega a una persona con cui hai
                  parlato: più la conversazione è fitta, più il filo è acceso e
                  più il nodo è grande. Trascina per ruotare.
                </p>
              </div>

              <Suspense
                fallback={<div className="caricamento">Tesso i fili…</div>}
              >
                <GrafoContatti
                  contatti={rete}
                  io={utente}
                  temaChiaro={temaEffettivo === 'light'}
                />
              </Suspense>

              <div className="grafo-legenda">
                <span>
                  <i
                    className="pallino-legenda"
                    style={{ background: utente?.avatarColor }}
                  />
                  Tu
                </span>
                {rete.map((c) => (
                  <span key={c.id}>
                    <i
                      className="pallino-legenda"
                      style={{ background: c.avatarColor }}
                    />
                    {c.displayName} · {c.scambiati}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="riquadro-filo">
            In totale hai scambiato <strong>{dati.inviati + dati.ricevuti}</strong>{' '}
            messaggi lungo <strong>{dati.chatAperte}</strong>{' '}
            {dati.chatAperte === 1 ? 'filo' : 'fili'}.
          </div>

          <div className="riga-azioni">
            <button
              type="button"
              className="bottone-principale"
              style={{ padding: '11px 18px' }}
              onClick={inviaReport}
              disabled={invioInCorso}
            >
              {invioInCorso ? 'Invio in corso…' : 'Inviami il report via email'}
            </button>
            <span className="nota-ai">
              Arriverà all&apos;indirizzo con cui ti sei registrato.
            </span>
          </div>
        </>
      )}
    </div>
  )
}
