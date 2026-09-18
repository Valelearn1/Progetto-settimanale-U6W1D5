/**
 * Le spunte di consegna e lettura.
 *
 * Disegnate in SVG e non con i caratteri "✓✓": con il testo la distanza fra i
 * due segni dipende dal font di sistema, quindi cambia fra Mac, Windows e
 * Android, e l'unico modo di avvicinarli sarebbe un letter-spacing negativo
 * che su certi font li fa sovrapporre male. Qui le coordinate sono nostre e il
 * risultato è identico ovunque.
 *
 * Nella doppia, il secondo segno parte a metà del primo: è la sovrapposizione
 * che rende la coppia riconoscibile a colpo d'occhio anche a 11px.
 */
export default function Spunta({ letta }) {
  return (
    <svg
      className={`spunta${letta ? ' letta' : ''}`}
      viewBox="0 0 18 11"
      width={letta ? 17 : 11}
      height={11}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      role="img"
      aria-label={letta ? 'Letto' : 'Inviato'}
    >
      <title>{letta ? 'Letto' : 'Inviato'}</title>
      {/* Il primo segno compare solo nella doppia */}
      {letta && <path d="M1 6.2 L3.9 9.2 L9.6 1.8" />}
      <path d={letta ? 'M6.6 6.2 L9.5 9.2 L16.4 1.8' : 'M1.2 6.2 L4.1 9.2 L10 1.8'} />
    </svg>
  )
}
