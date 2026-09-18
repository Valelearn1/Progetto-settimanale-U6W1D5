package com.example.demo.dto.response;

import java.util.UUID;

/**
 * Un nodo del grafo dei contatti: con chi hai parlato e quanto fitto è il filo
 * che vi lega.
 *
 * <p>{@code scambiati} conta TUTTI i messaggi della conversazione, non solo i
 * tuoi: è l'intensità dello scambio, non di quanto hai scritto tu — quella è
 * già la statistica "messaggi inviati".
 */
public record PeerStatsDTO(
        UUID id,
        String username,
        String displayName,
        String avatarColor,
        long scambiati) {
}
