/**
 * Distribuisce N punti su una sfera con la spirale di Fibonacci.
 *
 * Perché non una griglia latitudine/longitudine: quella addensa i punti ai poli
 * e li dirada all'equatore, e con pochi contatti il grafo risulta storto.
 * La spirale li tiene a distanza pressoché uguale qualunque sia N — che è
 * esattamente ciò che serve, visto che il numero di contatti cambia da utente
 * a utente.
 *
 * L'angolo aureo (π · (3 − √5) ≈ 2,39967 rad) è ciò che impedisce ai punti di
 * allinearsi in raggi visibili: qualunque frazione razionale del giro creerebbe
 * file riconoscibili.
 */
export function posizioniSuSfera(quanti, raggio) {
  if (quanti === 1) return [[0, raggio * 0.62, 0]]

  const angoloAureo = Math.PI * (3 - Math.sqrt(5))
  const passo = 2 / quanti

  return Array.from({ length: quanti }, (_, i) => {
    const y = i * passo - 1 + passo / 2
    const r = Math.sqrt(Math.max(0, 1 - y * y))
    const phi = i * angoloAureo
    return [Math.cos(phi) * r * raggio, y * raggio, Math.sin(phi) * r * raggio]
  })
}
