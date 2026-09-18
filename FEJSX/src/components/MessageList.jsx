import { useEffect, useRef } from 'react'

const ORA = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' })
const GIORNO = new Intl.DateTimeFormat('it-IT', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
})

function etichettaGiorno(data) {
  const oggi = new Date()
  const ieri = new Date()
  ieri.setDate(oggi.getDate() - 1)
  const stessoGiorno = (a, b) => a.toDateString() === b.toDateString()

  if (stessoGiorno(data, oggi)) return 'Oggi'
  if (stessoGiorno(data, ieri)) return 'Ieri'
  return GIORNO.format(data)
}

export default function MessageList({ messaggi, ioId }) {
  const fondo = useRef(null)

  // Si scorre in fondo a ogni messaggio nuovo: è il comportamento che ci si
  // aspetta da una chat, e senza questo i messaggi arrivano fuori campo.
  useEffect(() => {
    fondo.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messaggi])

  let ultimoGiorno = null

  return (
    <div className="messaggi">
      {messaggi.map((m) => {
        const data = new Date(m.sentAt)
        const giorno = data.toDateString()
        const nuovoGiorno = giorno !== ultimoGiorno
        ultimoGiorno = giorno

        const mio = m.senderId === ioId

        return (
          <div key={m.id} style={{ display: 'contents' }}>
            {nuovoGiorno && (
              <div className="separatore-data">{etichettaGiorno(data)}</div>
            )}
            <div className={`bolla ${mio ? 'inviata' : 'ricevuta'}`}>
              {m.content}
              <div className="bolla-meta">
                <span>{ORA.format(data)}</span>
                {/* La doppia spunta ha senso solo sui messaggi che ho mandato io:
                    sapere di aver letto i miei non serve a nessuno. */}
                {mio && (
                  <span
                    className={`spunta${m.readAt ? ' letta' : ''}`}
                    title={m.readAt ? 'Letto' : 'Inviato'}
                  >
                    {m.readAt ? '✓✓' : '✓'}
                  </span>
                )}
              </div>
            </div>
          </div>
        )
      })}
      <div ref={fondo} />
    </div>
  )
}
