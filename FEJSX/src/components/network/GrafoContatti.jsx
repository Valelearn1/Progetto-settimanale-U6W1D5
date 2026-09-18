import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { posizioniSuSfera } from './posizioni'

// Raggio della sfera su cui stanno i contatti, e distanza della camera.
// Il rapporto fra i due decide quanto respira la composizione: con la camera
// troppo vicina i nodi ai bordi finiscono tagliati fuori dal riquadro.
const RAGGIO_SFERA = 5.4
const DISTANZA_CAMERA = 14

/**
 * Il grafo dei contatti: tu al centro, un nodo per ogni persona con cui hai
 * parlato, e un filo rosso che vi lega.
 *
 * Il 3D qui codifica dati, non decora:
 *   - dimensione del nodo  -> messaggi scambiati con quella persona
 *   - opacità del filo     -> intensità della conversazione
 *   - numero di nodi       -> la statistica "chat aperte", resa visibile
 *
 * La rotazione fa un lavoro preciso: districa i fili che in due dimensioni si
 * sovrapporrebbero. Se il grafo fosse leggibile identico da fermo e piatto,
 * il 3D sarebbe decorazione e andrebbe tolto.
 *
 * Scritto con three.js "nudo" e non con @react-three/fiber, che alla data di
 * scrittura non supporta React 19.3 (vuole >=19 <19.3): forzare quel vincolo
 * avrebbe messo in casa un albero di dipendenze incoerente.
 */
export default function GrafoContatti({ contatti, io, temaChiaro }) {
  const contenitore = useRef(null)

  useEffect(() => {
    const host = contenitore.current
    if (!host || contatti.length === 0) return

    const larghezza = host.clientWidth
    const altezza = host.clientHeight

    /* ---------- scena ---------- */
    const scena = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(50, larghezza / altezza, 0.1, 100)
    camera.position.set(0, 0, DISTANZA_CAMERA)

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    renderer.setSize(larghezza, altezza)
    // Tetto a 2: oltre non si vede differenza e si raddoppiano i pixel da disegnare.
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    host.appendChild(renderer.domElement)

    scena.add(new THREE.AmbientLight(0xffffff, temaChiaro ? 1.5 : 0.9))
    const luce = new THREE.PointLight(0xffffff, temaChiaro ? 120 : 180)
    luce.position.set(8, 10, 12)
    scena.add(luce)

    /* ---------- nodo centrale: io ---------- */
    const coloreIo = new THREE.Color(io?.avatarColor ?? '#e0485b')
    const centro = new THREE.Mesh(
      new THREE.SphereGeometry(0.85, 32, 32),
      new THREE.MeshStandardMaterial({
        color: coloreIo,
        emissive: coloreIo,
        emissiveIntensity: 0.35,
        roughness: 0.4,
      }),
    )
    scena.add(centro)

    /* ---------- nodi e fili ---------- */
    const massimo = Math.max(...contatti.map((c) => c.scambiati), 1)
    const posizioni = posizioniSuSfera(contatti.length, RAGGIO_SFERA)
    const daRipulire = []

    contatti.forEach((contatto, i) => {
      const [x, y, z] = posizioni[i]
      const intensita = contatto.scambiati / massimo

      // Raggio fra 0.28 e 0.62: la differenza si vede, ma i nodi piccoli
      // restano cliccabili e leggibili.
      const raggio = 0.28 + intensita * 0.34
      const colore = new THREE.Color(contatto.avatarColor)
      const geometria = new THREE.SphereGeometry(raggio, 24, 24)
      const materiale = new THREE.MeshStandardMaterial({
        color: colore,
        emissive: colore,
        emissiveIntensity: 0.25,
        roughness: 0.45,
      })
      const nodo = new THREE.Mesh(geometria, materiale)
      nodo.position.set(x, y, z)
      scena.add(nodo)
      daRipulire.push(geometria, materiale)

      // Il filo rosso, curvo come nell'illustrazione e non teso: un arco
      // racconta un legame, un segmento racconta un grafico.
      //
      // E' un TUBO e non una Line perche' LineBasicMaterial ignora linewidth
      // su quasi tutte le piattaforme: resterebbe un capello da 1px,
      // praticamente invisibile. Il tubo ha spessore vero, e lo spessore puo'
      // quindi diventare un altro modo di dire "quanto e' fitta la chat".
      const arrivo = new THREE.Vector3(x, y, z)
      const meta = arrivo.clone().multiplyScalar(0.5)
      // Scostamento perpendicolare al raggio: da' la campata all'arco.
      const scostamento = new THREE.Vector3(-z, 0, x).normalize().multiplyScalar(1.15)
      const curva = new THREE.QuadraticBezierCurve3(
        new THREE.Vector3(0, 0, 0),
        meta.add(scostamento),
        arrivo,
      )

      const filoGeom = new THREE.TubeGeometry(curva, 26, 0.022 + intensita * 0.042, 8, false)
      // MeshBasic e non Standard: il filo deve brillare di luce propria come
      // nell'immagine, non essere illuminato dalla scena.
      const filoMat = new THREE.MeshBasicMaterial({
        color: temaChiaro ? 0xc0334a : 0xe0485b,
        transparent: true,
        // Parte da 0.35 anche per la conversazione più scarna: un filo
        // invisibile non racconterebbe che il legame esiste.
        opacity: 0.35 + intensita * 0.55,
      })
      scena.add(new THREE.Mesh(filoGeom, filoMat))
      daRipulire.push(filoGeom, filoMat)
    })

    /* ---------- controlli ---------- */
    const controlli = new OrbitControls(camera, renderer.domElement)
    controlli.enableDamping = true
    controlli.enablePan = false
    controlli.minDistance = 9
    controlli.maxDistance = 30

    // Chi ha chiesto meno animazioni si tiene la scena ferma, ruotabile a mano.
    const menoMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    controlli.autoRotate = !menoMovimento
    controlli.autoRotateSpeed = 0.7

    /* ---------- ciclo di disegno ---------- */
    let frame
    let attiva = true

    function disegna() {
      if (!attiva) return
      frame = requestAnimationFrame(disegna)
      controlli.update()
      renderer.render(scena, camera)
    }
    disegna()

    // Scheda in secondo piano: si smette di disegnare. Senza, la GPU continua
    // a lavorare per una pagina che nessuno sta guardando.
    function suVisibilita() {
      if (document.hidden) {
        attiva = false
        cancelAnimationFrame(frame)
      } else if (!attiva) {
        attiva = true
        disegna()
      }
    }
    document.addEventListener('visibilitychange', suVisibilita)

    function suRidimensiona() {
      const l = host.clientWidth
      const a = host.clientHeight
      camera.aspect = l / a
      camera.updateProjectionMatrix()
      renderer.setSize(l, a)
    }
    const osservatore = new ResizeObserver(suRidimensiona)
    osservatore.observe(host)

    /* ---------- smontaggio ---------- */
    return () => {
      attiva = false
      cancelAnimationFrame(frame)
      document.removeEventListener('visibilitychange', suVisibilita)
      osservatore.disconnect()
      controlli.dispose()
      // three.js non libera da solo la memoria della GPU: senza queste dispose
      // ogni rientro nella pagina lascerebbe dietro geometrie e materiali.
      daRipulire.forEach((r) => r.dispose())
      centro.geometry.dispose()
      centro.material.dispose()
      renderer.dispose()
      host.removeChild(renderer.domElement)
    }
  }, [contatti, io, temaChiaro])

  return <div ref={contenitore} className="grafo" />
}
