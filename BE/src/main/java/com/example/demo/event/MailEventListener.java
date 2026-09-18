package com.example.demo.event;

import com.example.demo.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailEventListener {

    private final MailService mailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistration(MailEvents.RegistrationRequested event) {
        invia(() -> mailService.sendRegistrationEmail(
                event.email(), event.displayName(), event.codice()), event.email());
    }

    /**
     * Se l'invio fallisce lo registriamo nei log senza propagare l'errore: il
     * dato e' gia' stato salvato e non ha senso annullarlo perche' Gmail era
     * irraggiungibile. L'utente potra' chiedere un nuovo codice.
     */
    private void invia(Runnable azione, String destinatario) {
        try {
            azione.run();
        } catch (Exception e) {
            log.error("Invio email a {} fallito: {}", destinatario, e.getMessage());
        }
    }
}
