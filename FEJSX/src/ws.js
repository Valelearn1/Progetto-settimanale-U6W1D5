import { Client } from '@stomp/stompjs'
import { WS_URL } from './api'

/**
 * Crea il client STOMP gia' iscritto ai tre canali dell'applicazione.
 *
 * Il token viaggia negli header del frame CONNECT e non nell'handshake HTTP:
 * l'API WebSocket del browser non permette di aggiungere header alla richiesta
 * di handshake, mentre gli header nativi del frame CONNECT sono a livello
 * applicativo e passano senza problemi.
 *
 * @param token  JWT dell'utente collegato
 * @param eventi { onMessaggio, onPresenza, onNuovoUtente, onErrore }
 */
export function creaClient(token, eventi) {
  const client = new Client({
    brokerURL: WS_URL,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},

    onConnect: () => {
      // Consegne mirate: i messaggi delle mie conversazioni e le conferme di lettura.
      client.subscribe('/user/queue/messages', (frame) => {
        const payload = JSON.parse(frame.body)
        // Il server manda un singolo messaggio all'invio e un ARRAY quando
        // segna letti piu' messaggi in un colpo solo: qui si normalizza.
        eventi.onMessaggio?.(Array.isArray(payload) ? payload : [payload])
      })

      // "Sta scrivendo": arriva solo a me, solo dall'altra persona.
      client.subscribe('/user/queue/typing', (frame) => {
        eventi.onScrittura?.(JSON.parse(frame.body))
      })

      // Broadcast: chi entra e chi esce.
      client.subscribe('/topic/presence', (frame) => {
        eventi.onPresenza?.(JSON.parse(frame.body))
      })

      // Broadcast: un nuovo utente ha confermato l'email ed entra in rubrica.
      client.subscribe('/topic/users', (frame) => {
        eventi.onNuovoUtente?.(JSON.parse(frame.body))
      })
    },

    onStompError: (frame) => eventi.onErrore?.(frame.headers.message ?? 'Errore STOMP'),
    onWebSocketError: () => eventi.onErrore?.('Connessione al server non riuscita'),
  })

  client.activate()
  return client
}

export function inviaMessaggio(client, chatId, content) {
  client.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ chatId, content }),
  })
}

/**
 * Annuncia che sto scrivendo (o che ho smesso).
 * Il chiamante deve limitarne la frequenza: un invio a ogni tasto premuto
 * inonderebbe il socket senza aggiungere informazione.
 */
export function segnalaScrittura(client, chatId, scrivendo) {
  client.publish({
    destination: '/app/chat.typing',
    body: JSON.stringify({ chatId, scrivendo }),
  })
}

export function segnalaLettura(client, chatId) {
  client.publish({
    destination: '/app/chat.read',
    body: JSON.stringify({ chatId }),
  })
}
