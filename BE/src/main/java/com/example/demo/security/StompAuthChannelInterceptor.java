package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Autentica la sessione WebSocket leggendo il JWT dal frame STOMP CONNECT e
 * attaccandolo alla sessione come Principal.
 *
 * <p>Due dettagli non ovvi, ed e' facile sbagliarli:
 *
 * <ol>
 *   <li><b>Il token viaggia nel frame CONNECT, non nell'handshake HTTP.</b>
 *       L'API WebSocket del browser non permette di aggiungere header alla
 *       richiesta di handshake. Gli header nativi del frame CONNECT sono invece
 *       a livello applicativo e passano senza problemi.</li>
 *   <li><b>Per questo /ws sta in permitAll()</b> nella SecurityFilterChain:
 *       l'handshake arriva senza Authorization, e se la catena di filtri HTTP
 *       lo bloccasse con 401 il CONNECT non verrebbe mai spedito.
 *       L'autenticazione vera e' questa qui, un gradino dopo.</li>
 * </ol>
 *
 * <p>Da questo momento il mittente di ogni messaggio lo decide il server
 * leggendo il Principal: il client non puo' dichiarare chi e'.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PREFISSO = "Bearer ";

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");

            if (header == null || !header.startsWith(PREFISSO)) {
                throw new MessagingException("Token mancante: impossibile aprire la sessione");
            }

            String username = jwtService.extractUsername(header.substring(PREFISSO.length()));
            if (username == null) {
                throw new MessagingException("Token non valido o scaduto");
            }

            accessor.setUser(new StompPrincipal(username));
        }

        return message;
    }
}
