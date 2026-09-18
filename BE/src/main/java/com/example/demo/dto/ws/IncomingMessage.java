package com.example.demo.dto.ws;

import java.util.UUID;

/**
 * Cio' che il client puo' dire quando invia un messaggio.
 *
 * <p>Nota che NON c'e' il mittente: quello lo ricaviamo dal Principal della
 * sessione, cosi' nessuno puo' fingersi un altro. Il server verifica inoltre
 * che chi scrive partecipi davvero alla chat indicata.
 */
public record IncomingMessage(UUID chatId, String content) {
}
