package com.example.demo.service;

import com.example.demo.config.OpenRouterProperties;
import com.example.demo.dto.openrouter.WireMessage;
import com.example.demo.dto.response.SuggestionResponseDTO;
import com.example.demo.entity.Chat;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Propone risposte per la conversazione aperta.
 *
 * <p><b>Requisito R4: nulla di quanto suggerito viene salvato.</b> Questa
 * classe dipende da {@link MessageRepository} e {@link ChatRepository} solo per
 * <em>leggere</em>: non ha entita' proprie, non chiama mai {@code save}, e il
 * metodo pubblico e' annotato {@code readOnly = true} — se un giorno qualcuno
 * ci aggiungesse una scrittura, la transazione la rifiuterebbe.
 *
 * <p>Se l'utente sceglie una proposta, il frontend la scrive nella casella di
 * testo: da li' in poi e' un messaggio come gli altri, salvato solo quando
 * l'utente preme invio — e a quel punto e' suo, non dell'AI.
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private static final int QUANTE = 3;
    private static final int LUNGHEZZA_MASSIMA = 200;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final OpenRouterService openRouterService;
    private final OpenRouterProperties properties;

    @Transactional(readOnly = true)
    public SuggestionResponseDTO suggerisci(User io, UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione non trovata"));

        if (!chat.partecipa(io)) {
            throw new AccessDeniedException("Questa conversazione non ti riguarda");
        }

        User altro = chat.altroRispettoA(io);

        // Gli ultimi N, dal piu' recente: vanno poi rovesciati per ridare al
        // modello l'ordine cronologico in cui la conversazione e' avvenuta.
        List<Message> ultimi = new ArrayList<>(messageRepository.findByChat_IdOrderBySentAtDesc(
                chatId, PageRequest.of(0, properties.getMaxHistoryMessages())));
        java.util.Collections.reverse(ultimi);

        List<WireMessage> conversazione = new ArrayList<>();
        conversazione.add(WireMessage.system(istruzioni(io, altro, ultimi.isEmpty())));

        // Il punto di vista viene ribaltato: cio' che ha detto l'altro diventa
        // "user", cio' che ho detto io diventa "assistant". Il modello vede la
        // conversazione come se fosse lui a doverla proseguire.
        for (Message m : ultimi) {
            conversazione.add(m.getSender().getId().equals(io.getId())
                    ? WireMessage.assistant(m.getContent())
                    : WireMessage.user(m.getContent()));
        }

        // L'ultimo turno DEVE essere dell'utente: senza, il provider rifiuta la
        // richiesta con "No user query found in messages" — cosa che succede
        // sempre a conversazione vuota, e ogni volta che l'ultimo messaggio
        // l'ho scritto io. Chiudere con l'istruzione ha anche il vantaggio di
        // ripetere il formato richiesto subito prima della generazione.
        conversazione.add(WireMessage.user(richiestaFinale(altro, ultimi.isEmpty())));

        String grezzo = openRouterService.completa(conversazione);
        return new SuggestionResponseDTO(analizza(grezzo), properties.getModelLabel());
    }

    private String istruzioni(User io, User altro, boolean conversazioneVuota) {
        if (conversazioneVuota) {
            return """
                    Sei %s e stai per scrivere il PRIMO messaggio a %s in una chat privata.
                    Proponi ESATTAMENTE 3 modi diversi di rompere il ghiaccio, uno per riga.
                    Nessuna numerazione, nessun elenco puntato, nessuna virgoletta,
                    nessun commento prima o dopo. Ogni proposta al massimo 140 caratteri.
                    Scrivi in italiano, in tono colloquiale.
                    """.formatted(io.getDisplayName(), altro.getDisplayName());
        }
        return """
                Sei %s e stai rispondendo a %s in una chat privata.
                Leggi la conversazione e proponi ESATTAMENTE 3 risposte possibili,
                diverse fra loro per tono o contenuto, una per riga.
                Nessuna numerazione, nessun elenco puntato, nessuna virgoletta,
                nessun commento prima o dopo. Ogni proposta al massimo 140 caratteri.
                Usa la stessa lingua della conversazione.
                """.formatted(io.getDisplayName(), altro.getDisplayName());
    }

    /** L'istruzione conclusiva, quella a cui il modello risponde davvero. */
    private String richiestaFinale(User altro, boolean conversazioneVuota) {
        return conversazioneVuota
                ? ("Scrivi ora 3 modi diversi di iniziare la conversazione con "
                        + altro.getDisplayName() + ", uno per riga, senza numerazione.")
                : "Scrivi ora le 3 risposte, una per riga, senza numerazione e senza commenti.";
    }

    /**
     * Da testo libero a lista di proposte.
     *
     * <p>Il modello riceve istruzioni precise ma non e' tenuto a rispettarle:
     * puo' numerare, mettere trattini, aggiungere virgolette o una frase di
     * accompagnamento. Qui si ripulisce quello che arriva davvero, invece di
     * fidarsi del formato richiesto.
     */
    private List<String> analizza(String grezzo) {
        return grezzo.lines()
                .map(String::strip)
                // toglie "1." "2)" "- " "* " e le virgolette di apertura/chiusura
                .map(r -> r.replaceFirst("^\\s*(\\d+\\s*[.)\\-]|[-*•])\\s*", ""))
                .map(r -> r.replaceAll("^[\"'«“”]+|[\"'»“”]+$", ""))
                .map(String::strip)
                .filter(r -> !r.isBlank())
                .map(r -> r.length() > LUNGHEZZA_MASSIMA
                        ? r.substring(0, LUNGHEZZA_MASSIMA).strip() + "…"
                        : r)
                .distinct()
                .limit(QUANTE)
                .toList();
    }
}
