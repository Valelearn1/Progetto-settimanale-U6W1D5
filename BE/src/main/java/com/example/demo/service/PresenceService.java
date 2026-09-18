package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiene traccia di chi e' collegato.
 *
 * <p>Lo stato vive <b>in memoria</b>, non sul database: se il server riparte,
 * riparte anche la presenza — ed e' corretto cosi', perche' dopo un riavvio
 * nessuna sessione WebSocket e' piu' viva. Salvarla su PostgreSQL lascerebbe
 * righe che dicono "online" per utenti scollegati da ore, e nessuno le
 * ripulirebbe.
 *
 * <p>Il conteggio per utente serve perche' la stessa persona puo' avere piu'
 * schede aperte: va dichiarata offline solo quando si chiude l'ultima. Con un
 * semplice booleano, chiudere la terza scheda la farebbe sparire pur essendo
 * ancora collegata dalle altre due.
 *
 * <p>Le due mappe sono {@link ConcurrentHashMap} perche' connessioni e
 * disconnessioni arrivano da thread diversi del broker.
 */
@Service
public class PresenceService {

    private final Map<String, String> sessioneUtente = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessioniPerUtente = new ConcurrentHashMap<>();

    /** @return true se l'utente e' passato da offline a online adesso. */
    public boolean connetti(String sessionId, String username) {
        sessioneUtente.put(sessionId, username);
        return sessioniPerUtente.merge(username, 1, Integer::sum) == 1;
    }

    /** @return lo username se era l'ultima sessione aperta, altrimenti null. */
    public String disconnetti(String sessionId) {
        String username = sessioneUtente.remove(sessionId);
        if (username == null) {
            return null;
        }

        // computeIfPresent con null come risultato rimuove la chiave: cosi' la
        // mappa non accumula utenti a quota zero.
        Integer rimaste = sessioniPerUtente.computeIfPresent(
                username, (k, v) -> v > 1 ? v - 1 : null);

        return rimaste == null ? username : null;
    }

    public Set<String> online() {
        return Set.copyOf(sessioniPerUtente.keySet());
    }

    public boolean isOnline(String username) {
        return sessioniPerUtente.containsKey(username);
    }
}
