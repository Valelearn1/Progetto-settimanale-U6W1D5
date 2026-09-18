const BASE = 'http://localhost:8080'

export const WS_URL = `${BASE}/ws`.replace('http', 'ws')

/**
 * Il backend risponde sempre in JSON, anche sugli errori:
 * { stato, messaggio, istante }. Qui quel messaggio viene trasformato in una
 * Error, cosi' ogni schermata puo' limitarsi a un try/catch invece di
 * controllare response.ok ogni volta.
 */
async function comeJson(response) {
  if (!response.ok) {
    let dettaglio = `Richiesta fallita (${response.status})`
    try {
      const corpo = await response.json()
      if (corpo?.messaggio) dettaglio = corpo.messaggio
    } catch {
      // risposta senza corpo JSON: resta il messaggio generico
    }
    const errore = new Error(dettaglio)
    errore.stato = response.status
    throw errore
  }
  // 202 e 204 possono non avere corpo
  const testo = await response.text()
  return testo ? JSON.parse(testo) : null
}

function intestazioni(token, conCorpo = false) {
  const h = {}
  if (conCorpo) h['Content-Type'] = 'application/json'
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

const get = (path, token) =>
  fetch(`${BASE}${path}`, { headers: intestazioni(token) }).then(comeJson)

const post = (path, corpo, token) =>
  fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: intestazioni(token, true),
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  }).then(comeJson)

// === Autenticazione (pubblica) ===
export const registrati = (dati) => post('/api/auth/register', dati)
export const verifica = (email, codice) => post('/api/auth/verify', { email, codice })
export const accedi = (email, password) => post('/api/auth/login', { email, password })

// === Utenti ===
export const rubrica = (token) => get('/api/users', token)
export const io = (token) => get('/api/users/me', token)

// === Chat ===
export const mieChat = (token) => get('/api/chats', token)
export const apriChat = (peerId, token) => post('/api/chats', { peerId }, token)
export const messaggiDi = (chatId, token) => get(`/api/chats/${chatId}/messages`, token)

// === Presenza ===
export const chiEOnline = (token) => get('/api/presence', token)

// === Suggerimenti AI (nulla viene salvato lato server) ===
export const suggerisci = (chatId, token) => post('/api/suggestions', { chatId }, token)

// === Statistiche ===
export const statistiche = (token) => get('/api/stats', token)
export const statistichePerEmail = (token) => post('/api/stats/email', undefined, token)
export const reteContatti = (token) => get('/api/stats/network', token)
