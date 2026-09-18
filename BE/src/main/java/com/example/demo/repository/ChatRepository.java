package com.example.demo.repository;

import com.example.demo.dto.response.PeerStatsDTO;
import com.example.demo.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * La conversazione fra due persone, cercata per chiave di coppia.
     * La chiave e' costruita ordinando i due id, quindi e' identica nei due versi.
     */
    Optional<Chat> findByPairKey(String pairKey);

    /** Le mie conversazioni, la piu' attiva per prima. */
    @Query("""
            SELECT c FROM Chat c
            JOIN c.participants p
            WHERE p.id = :me
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    List<Chat> findMie(@Param("me") UUID me);

    /**
     * Quante conversazioni ho davvero aperto: una chat creata e mai usata non
     * conta. Il filtro sfrutta lastMessageAt, che e' gia' valorizzato, invece
     * di andare a contare i messaggi.
     */
    @Query("""
            SELECT COUNT(c) FROM Chat c
            JOIN c.participants p
            WHERE p.id = :me AND c.lastMessageAt IS NOT NULL
            """)
    long countChatAperte(@Param("me") UUID me);

    /**
     * I nodi del grafo dei contatti: un contatto per riga, con quanti messaggi
     * contiene in tutto la conversazione che avete.
     *
     * <p>Le DUE join sulla stessa collezione non sono un errore: la prima
     * seleziona le chat in cui compaio io, la seconda pesca l'altro
     * partecipante della stessa chat. Con conversazioni a due, "l'altro" e'
     * sempre esattamente uno.
     *
     * <p>La LEFT JOIN sui messaggi tiene dentro anche le chat vuote, che
     * risultano con COUNT 0: e' il frontend a decidere se mostrarle.
     */
    @Query("""
            SELECT new com.example.demo.dto.response.PeerStatsDTO(
                       altro.id, altro.username, altro.displayName,
                       altro.avatarColor, COUNT(m))
            FROM Chat c
            JOIN c.participants io
            JOIN c.participants altro
            LEFT JOIN Message m ON m.chat = c
            WHERE io.id = :me AND altro.id <> :me
            GROUP BY altro.id, altro.username, altro.displayName, altro.avatarColor
            ORDER BY COUNT(m) DESC
            """)
    List<PeerStatsDTO> reteDiContatti(@Param("me") UUID me);
}
