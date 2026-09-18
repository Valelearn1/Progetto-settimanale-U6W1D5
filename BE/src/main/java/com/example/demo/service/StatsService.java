package com.example.demo.service;

import com.example.demo.dto.response.PeerStatsDTO;
import com.example.demo.dto.response.StatsResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.event.MailEvents;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public StatsResponseDTO calcola(User io) {
        return new StatsResponseDTO(
                messageRepository.countBySender_Id(io.getId()),
                messageRepository.countRicevuti(io.getId()),
                chatRepository.countChatAperte(io.getId()),
                io.getCreatedAt());
    }

    /** I nodi del grafo: i contatti con cui hai un filo, il piu' fitto per primo. */
    @Transactional(readOnly = true)
    public List<PeerStatsDTO> rete(User io) {
        return chatRepository.reteDiContatti(io.getId());
    }

    /**
     * Calcola le statistiche e ne chiede l'invio per email.
     *
     * <p>{@code @Transactional} non e' decorativo: l'evento viene consegnato
     * AFTER_COMMIT, e senza una transazione attiva
     * {@code @TransactionalEventListener} lo scarterebbe in silenzio — l'email
     * non partirebbe e nessun errore lo direbbe.
     */
    @Transactional(readOnly = true)
    public StatsResponseDTO inviaPerEmail(User io) {
        StatsResponseDTO stats = calcola(io);

        events.publishEvent(new MailEvents.StatsRequested(
                io.getEmail(),
                io.getDisplayName(),
                stats.inviati(),
                stats.ricevuti(),
                stats.chatAperte(),
                stats.dal()));

        return stats;
    }
}
