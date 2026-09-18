package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Tutti i messaggi di una conversazione, dal piu' vecchio. */
    List<Message> findByChat_IdOrderBySentAtAsc(UUID chatId);

    /** Gli ultimi messaggi di una conversazione: cronologia per il suggerimento AI. */
    List<Message> findByChat_IdOrderBySentAtDesc(UUID chatId, org.springframework.data.domain.Pageable pageable);

    /** I messaggi di questa chat che non ho scritto io e non ho ancora aperto. */
    @Query("""
            SELECT m FROM Message m
            WHERE m.chat.id = :chatId AND m.sender.id <> :me AND m.readAt IS NULL
            ORDER BY m.sentAt ASC
            """)
    List<Message> findNonLetti(@Param("chatId") UUID chatId, @Param("me") UUID me);

    /**
     * Il contatore del badge in sidebar.
     *
     * <p>Esiste accanto a findNonLetti di proposito: quella carica gli oggetti
     * per segnarli letti e rispedirli aggiornati, questa serve solo al numerino.
     * Caricare le entita' per poi contarle e buttarle sarebbe lo spreco piu'
     * facile da introdurre in tutta l'applicazione.
     */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.chat.id = :chatId AND m.sender.id <> :me AND m.readAt IS NULL
            """)
    long contaNonLetti(@Param("chatId") UUID chatId, @Param("me") UUID me);

    // === Statistiche (§4.6 della progettazione) ===

    long countBySender_Id(UUID me);

    /** Ricevuti = tutto cio' che sta nelle mie chat e NON l'ho scritto io. */
    @Query("""
            SELECT COUNT(m) FROM Message m
            JOIN m.chat c
            JOIN c.participants p
            WHERE p.id = :me AND m.sender.id <> :me
            """)
    long countRicevuti(@Param("me") UUID me);
}
