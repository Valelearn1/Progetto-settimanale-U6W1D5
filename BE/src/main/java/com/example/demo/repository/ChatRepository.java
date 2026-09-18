package com.example.demo.repository;

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
}
