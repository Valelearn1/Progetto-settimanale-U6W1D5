package com.example.demo.dto.response;

import java.time.Instant;

/**
 * Le tre statistiche richieste, piu' la data da cui sono calcolate.
 *
 * <p>Non esiste nessuna tabella che le conservi: sono dati derivati, ricavati
 * con tre COUNT a ogni richiesta. Un contatore salvato sarebbe piu' veloce ma
 * andrebbe fuori sincrono al primo messaggio che entra per un'altra strada.
 */
public record StatsResponseDTO(
        long inviati,
        long ricevuti,
        long chatAperte,
        Instant dal) {

    public long totale() {
        return inviati + ricevuti;
    }
}
