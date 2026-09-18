/**
 * Le iniziali su fondo colorato: l'avatarColor arriva dal backend ed è
 * assegnato a rotazione alla registrazione, così ogni contatto è
 * riconoscibile a colpo d'occhio senza caricare immagini.
 *
 * `online` accetta anche undefined: in quel caso il pallino non compare
 * affatto, perché "non lo so" è diverso da "è offline".
 */
export default function Avatar({ utente, dimensione = 36, online }) {
  const iniziali = (utente?.displayName ?? utente?.username ?? '?')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parola) => parola[0])
    .join('')
    .toUpperCase()

  return (
    <div
      className="avatar"
      style={{
        width: dimensione,
        height: dimensione,
        background: utente?.avatarColor ?? 'var(--bg-hover)',
        fontSize: dimensione * 0.38,
      }}
      title={utente?.displayName}
    >
      {iniziali}
      {online !== undefined && (
        <span
          className={`pallino${online ? ' online' : ''}`}
          title={online ? 'Online' : 'Offline'}
        />
      )}
    </div>
  )
}
