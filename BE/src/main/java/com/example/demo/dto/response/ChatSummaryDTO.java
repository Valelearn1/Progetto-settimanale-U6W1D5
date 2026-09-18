package com.example.demo.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Una riga della sidebar: con chi sto parlando, l'ultima cosa detta e quanti
 * messaggi devo ancora leggere. Basta questa per disegnare tutta la colonna
 * sinistra senza aprire nessuna conversazione.
 */
public record ChatSummaryDTO(
        UUID id,
        UserDTO interlocutore,
        String ultimoMessaggio,
        Instant lastMessageAt,
        long nonLetti) {
}
