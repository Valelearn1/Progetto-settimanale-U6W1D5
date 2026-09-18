import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ThemeToggle from '../components/ThemeToggle'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const posizione = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errore, setErrore] = useState(null)
  const [inCorso, setInCorso] = useState(false)

  // Messaggio portato qui dalla registrazione o dalla verifica riuscita.
  const messaggio = posizione.state?.messaggio

  async function invia(evento) {
    evento.preventDefault()
    setErrore(null)
    setInCorso(true)
    try {
      await login(email.trim(), password)
      navigate('/chat', { replace: true })
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
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div className="marchio">
              <span className="marchio-quadrato">F</span>
              Filo Rosso
            </div>
            <ThemeToggle />
          </div>

          <div>
            <h1>Bentornato</h1>
            <p className="sottotitolo">
              Il filo che ti lega alle tue conversazioni.
            </p>
          </div>

          {messaggio && <div className="avviso ok">{messaggio}</div>}
          {errore && <div className="avviso">{errore}</div>}

          <div className="campo">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="campo">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="bottone-principale" disabled={inCorso}>
            {inCorso ? 'Accesso in corso…' : 'Accedi'}
          </button>

          <p className="sottotitolo">
            Non hai un account? <Link className="link" to="/registrazione">Registrati</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
