import { useEffect, useRef } from 'react'

const ORA = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' })
const GIORNO = new Intl.DateTimeFormat('it-IT', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
})

/** Oltre questo intervallo due messaggi non sono più "di seguito". */
const PAUSA_MASSIMA_MINUTI = 5

function etichettaGiorno(data) {
  const oggi = new Date()
  const ieri = new Date()
  ieri.setDate(oggi.getDate() - 1)
  const stessoGiorno = (a, b) => a.toDateString() === b.toDateString()

  if (stessoGiorno(data, oggi)) return 'Oggi'
  if (stessoGiorno(data, ieri)) return 'Ieri'
  return GIORNO.format(data)
}

export default function MessageList({ messaggi, ioId, staScrivendo }) {
  const fondo = useRef(null)

  // Si scorre in fondo a ogni messaggio nuovo: è il comportamento che ci si
  // aspetta da una chat, e senza questo i messaggi arrivano fuori campo.
  useEffect(() => {
    fondo.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messaggi, staScrivendo])

  return (
    <div className="messaggi">
      {messaggi.map((m, i) => {
        const data = new Date(m.sentAt)
        const precedente = messaggi[i - 1]
        const mio = m.senderId === ioId

        const nuovoGiorno =
          !precedente || new Date(precedente.sentAt).toDateString() !== data.toDateString()

        /*
          Messaggi "di seguito": stesso mittente, stesso giorno, a pochi minuti
          di distanza. Vengono avvicinati e mostrano un orario solo, quello
          dell'ultimo del gruppo. Tre bolle staccate con tre orari identici
          sono rumore visivo che non aggiunge nessuna informazione.
        */
        const diSeguito =
          !nuovoGiorno &&
          precedente &&
          precedente.senderId === m.senderId &&
          (data - new Date(precedente.sentAt)) / 60000 < PAUSA_MASSIMA_MINUTI

        const successivo = messaggi[i + 1]
        const chiudeGruppo =
          !successivo ||
          successivo.senderId !== m.senderId ||
          new Date(successivo.sentAt).toDateString() !== data.toDateString() ||
          (new Date(successivo.sentAt) - data) / 60000 >= PAUSA_MASSIMA_MINUTI

        return (
          <div key={m.id} style={{ display: 'contents' }}>
            {nuovoGiorno && (
              <div className="separatore-data">{etichettaGiorno(data)}</div>
            )}
            <div
              className={[
                'bolla',
                mio ? 'inviata' : 'ricevuta',
                diSeguito ? 'di-seguito' : '',
                chiudeGruppo ? 'chiude-gruppo' : '',
              ]
                .filter(Boolean)
                .join(' ')}
            >
              {m.content}
              {/* L'orario compare solo in fondo al gruppo. */}
              {chiudeGruppo && (
                <div className="bolla-meta">
                  <span>{ORA.format(data)}</span>
                  {/* La doppia spunta ha senso solo sui messaggi che ho mandato
                      io: sapere di aver letto i miei non serve a nessuno. */}
                  {mio && (
                    <span
                      className={`spunta${m.readAt ? ' letta' : ''}`}
                      title={m.readAt ? 'Letto' : 'Inviato'}
                    >
                      {m.readAt ? '✓✓' : '✓'}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>
        )
      })}

      {staScrivendo && (
        <div className="bolla ricevuta scrivendo" aria-live="polite">
          <span className="punto" />
          <span className="punto" />
          <span className="punto" />
        </div>
      )}

      <div ref={fondo} />
    </div>
  )
}
