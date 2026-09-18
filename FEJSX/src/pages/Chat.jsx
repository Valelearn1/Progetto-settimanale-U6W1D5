import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Avatar from '../components/Avatar'
import Composer from '../components/Composer'
import MessageList from '../components/MessageList'
import Sidebar from '../components/Sidebar'
import SuggestionBar from '../components/SuggestionBar'
import { useAuth } from '../context/AuthContext'
import {
  apriChat,
  cambiaPreferita,
  chiEOnline,
  messaggiDi,
  mieChat,
  rubrica,
  suggerisci,
} from '../api'
import {
  creaClient,
  inviaMessaggio,
  segnalaLettura,
  segnalaScrittura,
} from '../ws'

/** Dopo quanto silenzio si smette di dire "sta scrivendo". */
const PAUSA_SCRITTURA = 2500

export default function Chat() {
  const { token, utente, logout } = useAuth()

  const [chat, setChat] = useState([])
  const [contatti, setContatti] = useState([])
  const [online, setOnline] = useState(new Set())
  const [chatAttiva, setChatAttiva] = useState(null)
  const [messaggi, setMessaggi] = useState([])
  const [testo, setTesto] = useState('')
  const [errore, setErrore] = useState(null)
  const [caricando, setCaricando] = useState(true)

  const [proposte, setProposte] = useState([])
  const [modello, setModello] = useState(null)
  const [aiInCorso, setAiInCorso] = useState(false)
  const [aiErrore, setAiErrore] = useState(null)

  // Chi sta scrivendo, per chat. È uno stato effimero: non viene mai salvato
  // né richiesto al server all'avvio.
  const [scrivendo, setScrivendo] = useState({})
  const timerScrittura = useRef(null)
  const scritturaAnnunciata = useRef(false)

  const client = useRef(null)
  // I gestori STOMP sono registrati una volta sola alla connessione, quindi
  // catturerebbero il valore iniziale di chatAttiva. La ref segue lo stato e
  // dà loro sempre il valore corrente.
  const chatAttivaRef = useRef(null)
  useEffect(() => {
    chatAttivaRef.current = chatAttiva
  }, [chatAttiva])

  /* ---------- caricamento iniziale ---------- */

  useEffect(() => {
    let annullato = false
    ;(async () => {
      try {
        const [mie, tutti, presenti] = await Promise.all([
          mieChat(token),
          rubrica(token),
          chiEOnline(token),
        ])
        if (annullato) return
        setChat(mie)
        setContatti(tutti)
        setOnline(new Set(presenti))
      } catch (e) {
        if (!annullato) setErrore(e.message)
      } finally {
        if (!annullato) setCaricando(false)
      }
    })()
    return () => {
      annullato = true
    }
  }, [token])

  /* ---------- socket ---------- */

  const suMessaggi = useCallback(
    (arrivati) => {
      // Il payload è sempre normalizzato ad array: un messaggio nuovo, oppure
      // il gruppo di messaggi appena segnati come letti.
      setMessaggi((precedenti) => {
        const dellaChatAperta = arrivati.filter(
          (m) => m.chatId === chatAttivaRef.current,
        )
        if (dellaChatAperta.length === 0) return precedenti

        const perId = new Map(precedenti.map((m) => [m.id, m]))
        dellaChatAperta.forEach((m) => perId.set(m.id, m))
        return [...perId.values()].sort(
          (a, b) => new Date(a.sentAt) - new Date(b.sentAt),
        )
      })

      // La sidebar si aggiorna sempre, anche per le conversazioni chiuse:
      // è lì che compare il badge dei non letti.
      setChat((precedenti) =>
        precedenti
          .map((c) => {
            const suoi = arrivati.filter((m) => m.chatId === c.id)
            if (suoi.length === 0) return c

            const ultimo = suoi[suoi.length - 1]
            const eAperta = c.id === chatAttivaRef.current
            const nuoviRicevuti = suoi.filter(
              (m) => m.senderId !== utente.id && !m.readAt,
            ).length

            return {
              ...c,
              ultimoMessaggio: ultimo.content,
              lastMessageAt: ultimo.sentAt,
              nonLetti: eAperta ? 0 : c.nonLetti + nuoviRicevuti,
            }
          })
          .sort((a, b) => new Date(b.lastMessageAt ?? 0) - new Date(a.lastMessageAt ?? 0)),
      )

      // Se sto guardando la conversazione, i messaggi arrivati sono già letti.
      const nuoviDaAltri = arrivati.some(
        (m) => m.chatId === chatAttivaRef.current && m.senderId !== utente.id && !m.readAt,
      )
      if (nuoviDaAltri && client.current?.connected) {
        segnalaLettura(client.current, chatAttivaRef.current)
      }
    },
    [utente],
  )

  useEffect(() => {
    if (!token) return

    client.current = creaClient(token, {
      onMessaggio: suMessaggi,
      onPresenza: ({ username, online: attivo }) =>
        setOnline((precedenti) => {
          const nuovo = new Set(precedenti)
          if (attivo) nuovo.add(username)
          else nuovo.delete(username)
          return nuovo
        }),
      onScrittura: ({ chatId, scrivendo: attivo }) => {
        setScrivendo((precedenti) => ({ ...precedenti, [chatId]: attivo }))
        // Rete di sicurezza: se il "ho smesso" si perde per strada, l'indicatore
        // resterebbe acceso per sempre. Scade da solo.
        if (attivo) {
          setTimeout(
            () => setScrivendo((p) => ({ ...p, [chatId]: false })),
            PAUSA_SCRITTURA + 1500,
          )
        }
      },
      onNuovoUtente: (u) =>
        setContatti((precedenti) =>
          precedenti.some((c) => c.id === u.id) || u.id === utente.id
            ? precedenti
            : [...precedenti, u],
        ),
      onErrore: setErrore,
    })

    return () => {
      client.current?.deactivate()
      client.current = null
    }
  }, [token, suMessaggi, utente])

  /* ---------- apertura di una conversazione ---------- */

  const apri = useCallback(
    async (chatId) => {
      setChatAttiva(chatId)
      scritturaAnnunciata.current = false
      clearTimeout(timerScrittura.current)
      setProposte([])
      setAiErrore(null)
      setTesto('')
      try {
        const storico = await messaggiDi(chatId, token)
        setMessaggi(storico)
        setChat((precedenti) =>
          precedenti.map((c) => (c.id === chatId ? { ...c, nonLetti: 0 } : c)),
        )
        if (client.current?.connected) {
          segnalaLettura(client.current, chatId)
        }
      } catch (e) {
        setErrore(e.message)
      }
    },
    [token],
  )

  const nuovaConversazione = useCallback(
    async (peerId) => {
      try {
        const creata = await apriChat(peerId, token)
        setChat((precedenti) =>
          precedenti.some((c) => c.id === creata.id) ? precedenti : [creata, ...precedenti],
        )
        apri(creata.id)
      } catch (e) {
        setErrore(e.message)
      }
    },
    [token, apri],
  )

  /* ---------- invio ---------- */

  function invia() {
    const contenuto = testo.trim()
    if (!contenuto || !chatAttiva || !client.current?.connected) return
    inviaMessaggio(client.current, chatAttiva, contenuto)
    fermaAnnuncioScrittura()
    setTesto('')
    setProposte([])
  }

  /**
   * Annuncia "sto scrivendo" al primo carattere e non a ogni tasto: il socket
   * riceverebbe decine di messaggi identici senza aggiungere informazione.
   * Il "ho smesso" parte da solo dopo qualche secondo di silenzio.
   */
  function suDigitazione(nuovoTesto) {
    setTesto(nuovoTesto)
    if (!chatAttiva || !client.current?.connected) return

    if (!scritturaAnnunciata.current && nuovoTesto.trim()) {
      segnalaScrittura(client.current, chatAttiva, true)
      scritturaAnnunciata.current = true
    }

    clearTimeout(timerScrittura.current)
    timerScrittura.current = setTimeout(fermaAnnuncioScrittura, PAUSA_SCRITTURA)
  }

  function fermaAnnuncioScrittura() {
    clearTimeout(timerScrittura.current)
    if (scritturaAnnunciata.current && chatAttiva && client.current?.connected) {
      segnalaScrittura(client.current, chatAttiva, false)
    }
    scritturaAnnunciata.current = false
  }

  async function togglePreferita(chatId) {
    try {
      const aggiornata = await cambiaPreferita(chatId, token)
      // La sidebar viene ricaricata dal server: l'ordine (preferite in cima,
      // poi per attività) lo decide lui, e rifarlo qui lo farebbe divergere.
      setChat(await mieChat(token))
      return aggiornata
    } catch (e) {
      setErrore(e.message)
    }
  }

  /* ---------- suggerimenti AI ---------- */

  async function chiediSuggerimenti() {
    if (!chatAttiva) return
    setAiInCorso(true)
    setAiErrore(null)
    try {
      const risposta = await suggerisci(chatAttiva, token)
      setProposte(risposta.proposte)
      setModello(risposta.modello)
    } catch (e) {
      setAiErrore(e.message)
      setProposte([])
    } finally {
      setAiInCorso(false)
    }
  }

  /* ---------- dati derivati ---------- */

  const conversazione = useMemo(
    () => chat.find((c) => c.id === chatAttiva) ?? null,
    [chat, chatAttiva],
  )

  /*
    Il totale dei non letti finisce nel titolo della scheda: è l'unico modo di
    accorgersi di un messaggio quando Filo Rosso è in secondo piano, senza
    chiedere il permesso per le notifiche di sistema.
  */
  const nonLettiTotali = useMemo(
    () => chat.reduce((somma, c) => somma + c.nonLetti, 0),
    [chat],
  )

  useEffect(() => {
    document.title = nonLettiTotali > 0 ? `(${nonLettiTotali}) Filo Rosso` : 'Filo Rosso'
    return () => {
      document.title = 'Filo Rosso'
    }
  }, [nonLettiTotali])

  const contattiSenzaChat = useMemo(() => {
    const conChat = new Set(chat.map((c) => c.interlocutore.id))
    return contatti.filter((u) => !conChat.has(u.id))
  }, [chat, contatti])

  if (caricando) {
    return <div className="caricamento">Carico le tue conversazioni…</div>
  }

  return (
    <div className="app">
      <Sidebar
        chat={chat}
        contattiSenzaChat={contattiSenzaChat}
        chatAttiva={chatAttiva}
        online={online}
        io={utente}
        onApriChat={apri}
        onNuovaChat={nuovaConversazione}
        onPreferita={togglePreferita}
        onEsci={logout}
      />

      <main className="conversazione">
        {errore && (
          <div className="avviso" style={{ margin: 12, borderRadius: 'var(--raggio)' }}>
            {errore}
          </div>
        )}

        {!conversazione ? (
          <div className="vuoto">
            <div>
              <h2>Nessuna conversazione aperta</h2>
              <p>
                Scegli una persona dalla colonna a sinistra. Il filo rosso
                comincia da lì.
              </p>
            </div>
          </div>
        ) : (
          <>
            <header className="conversazione-testata">
              <Avatar
                utente={conversazione.interlocutore}
                online={online.has(conversazione.interlocutore.username)}
              />
              <div>
                <div style={{ fontWeight: 600 }}>
                  {conversazione.interlocutore.displayName}
                </div>
                <div className="stato">
                  {online.has(conversazione.interlocutore.username)
                    ? 'Online'
                    : 'Offline'}
                </div>
              </div>
            </header>

            <MessageList
              messaggi={messaggi}
              ioId={utente.id}
              staScrivendo={Boolean(scrivendo[chatAttiva])}
            />

            <div className="composer">
              <SuggestionBar
                proposte={proposte}
                modello={modello}
                inCorso={aiInCorso}
                errore={aiErrore}
                onChiedi={chiediSuggerimenti}
                onScegli={(proposta) => {
                  setTesto(proposta)
                  setProposte([])
                }}
              />
              <Composer
                testo={testo}
                setTesto={suDigitazione}
                onInvia={invia}
                disabilitato={!chatAttiva}
              />
            </div>
          </>
        )}
      </main>
    </div>
  )
}
