package com.example.demo.listener;

import com.example.demo.dto.ws.PresenceUpdate;
import com.example.demo.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Spring pubblica eventi applicativi a ogni connessione e disconnessione STOMP.
 *
 * <p>Agganciandoci qui otteniamo la presenza senza che il client debba mandare
 * heartbeat applicativi: e' il ciclo di vita del socket a dirci la verita'.
 * Se il browser va in crash o la rete cade, il socket si chiude e l'evento
 * parte lo stesso — cosa che un "sono ancora qui" inviato dal client non
 * garantirebbe mai.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal utente = accessor.getUser();
        if (utente == null) {
            return;
        }

        // Annuncia solo il passaggio offline -> online: se apre la seconda
        // scheda, gli altri client hanno gia' il pallino acceso.
        if (presenceService.connetti(accessor.getSessionId(), utente.getName())) {
            annuncia(utente.getName(), true);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String username = presenceService.disconnetti(event.getSessionId());
        if (username != null) {
            annuncia(username, false);
        }
    }

    private void annuncia(String username, boolean online) {
        log.info("Presenza: '{}' e' {}", username, online ? "online" : "offline");
        messagingTemplate.convertAndSend("/topic/presence", new PresenceUpdate(username, online));
    }
}
