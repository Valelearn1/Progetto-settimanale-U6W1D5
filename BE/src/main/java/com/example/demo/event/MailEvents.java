package com.example.demo.event;

/**
 * Eventi pubblicati dai service quando c'e' una mail da inviare.
 *
 * <p>L'invio vero avviene solo dopo il COMMIT della transazione (vedi
 * {@link MailEventListener}): cosi' una SMTP lenta non tiene occupata la
 * connessione al database, e un errore di invio non annulla un'operazione
 * gia' andata a buon fine.
 *
 * <p>Il vantaggio di progetto e' che chi registra un utente non sa nulla di
 * SMTP: dichiara che <em>e' successo qualcosa</em>, non <em>cosa fare di
 * conseguenza</em>.
 */
public final class MailEvents {

    private MailEvents() {
    }

    public record RegistrationRequested(String email, String displayName, String codice) {
    }

    public record StatsRequested(
            String email,
            String displayName,
            long inviati,
            long ricevuti,
            long chatAperte,
            java.time.Instant dal) {
    }
}
