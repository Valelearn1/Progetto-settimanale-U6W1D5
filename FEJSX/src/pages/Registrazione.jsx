import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registrati } from '../api'

export default function Registrazione() {
  const navigate = useNavigate()
  const [dati, setDati] = useState({
    username: '',
    displayName: '',
    email: '',
    password: '',
  })
  const [errore, setErrore] = useState(null)
  const [inCorso, setInCorso] = useState(false)

  const aggiorna = (campo) => (e) =>
    setDati((precedente) => ({ ...precedente, [campo]: e.target.value }))

  async function invia(evento) {
    evento.preventDefault()
    setErrore(null)
    setInCorso(true)
    try {
      await registrati({
        ...dati,
        username: dati.username.trim(),
        email: dati.email.trim(),
      })
      navigate('/verifica', {
        state: { email: dati.email.trim() },
      })
    } catch (e) {
      setErrore(e.message)
    } finally {
      setInCorso(false)
    }
  }

  return (
    <div className="accesso">
      <div className="accesso-card">
        <form className="accesso-corpo" onSubmit={invia}>
          <div className="marchio">
            <span className="marchio-quadrato">F</span>
            Filo Rosso
          </div>

          <div>
            <h1>Crea il tuo account</h1>
            <p className="sottotitolo">
              Ti manderemo un codice via email per attivarlo.
            </p>
          </div>

          {errore && <div className="avviso">{errore}</div>}

          <div className="campo">
            <label htmlFor="displayName">Nome visualizzato</label>
            <input
              id="displayName"
              value={dati.displayName}
              onChange={aggiorna('displayName')}
              maxLength={60}
              required
            />
          </div>

          <div className="campo">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              value={dati.username}
              onChange={aggiorna('username')}
              /* Stesso vincolo del backend: lo username finisce nelle
                 destinazioni STOMP, certi caratteri darebbero problemi. */
              pattern="[a-zA-Z0-9._\-]+"
              minLength={3}
              maxLength={30}
              title="Lettere, numeri, punto, trattino e underscore"
              required
            />
          </div>

          <div className="campo">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={dati.email}
              onChange={aggiorna('email')}
              required
            />
          </div>

          <div className="campo">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              value={dati.password}
              onChange={aggiorna('password')}
              minLength={8}
              maxLength={72}
              required
            />
          </div>

          <button type="submit" className="bottone-principale" disabled={inCorso}>
            {inCorso ? 'Invio in corso…' : 'Registrati'}
          </button>

          <p className="sottotitolo">
            Hai già un account? <Link className="link" to="/login">Accedi</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
