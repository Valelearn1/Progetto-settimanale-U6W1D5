package com.example.demo.dto.ws;

import java.util.UUID;

/**
 * "Sto scrivendo" / "ho smesso".
 *
 * <p>Non viene salvato da nessuna parte ed e' volutamente effimero: vale finche'
 * arriva, poi scade da solo lato client. Un dato del genere sul database
 * sarebbe gia' vecchio nel momento in cui lo leggi.
 */
public record TypingUpdate(UUID chatId, String username, boolean scrivendo) {
}
