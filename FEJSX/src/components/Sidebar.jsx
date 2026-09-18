import { Link } from 'react-router-dom'
import Avatar from './Avatar'
import ThemeToggle from './ThemeToggle'

/**
 * Colonna sinistra: le conversazioni in corso e, sotto, i contatti con cui non
 * hai ancora parlato. Sono due elenchi distinti di proposito — mescolarli
 * farebbe perdere l'ordine per attività recente, che è l'unica ragione per cui
 * il backend tiene `lastMessageAt`.
 */
export default function Sidebar({
  chat,
  contattiSenzaChat,
  chatAttiva,
  online,
  io,
  onApriChat,
  onNuovaChat,
  onEsci,
}) {
  return (
    <aside className="sidebar filo-superiore">
      <div className="sidebar-testata">
        <div className="marchio">
          <span className="marchio-quadrato">F</span>
          Filo Rosso
        </div>
        <Link className="link" to="/statistiche" title="Le tue statistiche">
          Statistiche
        </Link>
      </div>

      <div className="sidebar-elenco">
        <div className="sidebar-sezione">Conversazioni</div>
        <div className="elenco-margine">
          {chat.length === 0 && (
            <p className="caricamento" style={{ padding: '4px 12px' }}>
              Nessuna conversazione. Scegli un contatto qui sotto.
            </p>
          )}
          {chat.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`riga${c.id === chatAttiva ? ' attiva' : ''}`}
              onClick={() => onApriChat(c.id)}
            >
              <Avatar
                utente={c.interlocutore}
                online={online.has(c.interlocutore.username)}
              />
              <span className="riga-testo">
                <span className="riga-nome">{c.interlocutore.displayName}</span>
                <span className="riga-anteprima">
                  {c.ultimoMessaggio ?? 'Nessun messaggio'}
                </span>
              </span>
              {c.nonLetti > 0 && <span className="badge">{c.nonLetti}</span>}
            </button>
          ))}
        </div>

        {contattiSenzaChat.length > 0 && (
          <>
            <div className="sidebar-sezione">Altri contatti</div>
            <div className="elenco-margine">
              {contattiSenzaChat.map((u) => (
                <button
                  key={u.id}
                  type="button"
                  className="riga"
                  onClick={() => onNuovaChat(u.id)}
                >
                  <Avatar utente={u} online={online.has(u.username)} />
                  <span className="riga-testo">
                    <span className="riga-nome">{u.displayName}</span>
                    <span className="riga-anteprima">@{u.username}</span>
                  </span>
                </button>
              ))}
            </div>
          </>
        )}
      </div>

      <div className="sidebar-pie">
        <Avatar utente={io} dimensione={32} online />
        <span className="nome">{io?.displayName}</span>
        <ThemeToggle />
        <button type="button" className="bottone-secondario" onClick={onEsci}>
          Esci
        </button>
      </div>
    </aside>
  )
}
