package com.example.demo.controller;

import com.example.demo.dto.response.MessageDTO;
import com.example.demo.dto.ws.IncomingMessage;
import com.example.demo.dto.ws.ReadRequest;
import com.example.demo.dto.ws.TypingUpdate;
import com.example.demo.entity.Chat;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.service.ChatService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    /** Destinazione a cui ogni client e' iscritto come /user/queue/messages. */
    private static final String CODA_UTENTE = "/queue/messages";

    private final ChatService chatService;
    private final UserService userService;
    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void invia(IncomingMessage incoming, Principal principal) {
        User mittente = userService.findByUsername(principal.getName());

        // 1. Salvare prima...
        MessageDTO messaggio = chatService.invia(mittente, incoming);

        // 2. ...consegnare poi. Due invii mirati, non un broadcast: uno al
        // destinatario e uno al mittente. Al mittente serve perche' id e sentAt
        // li assegna il database, quindi entrambe le interfacce si aggiornano
        // dalla stessa fonte e nessun altro utente riceve nulla.
        consegnaAEntrambi(incoming.chatId(), messaggio);
    }

    /**
     * Il destinatario dichiara di aver aperto la conversazione. I messaggi
     * aggiornati tornano a entrambi: a chi ha scritto per accendere la doppia
     * spunta, a chi legge per azzerare il contatore dei non letti.
     */
    @MessageMapping("/chat.read")
    public void letti(ReadRequest request, Principal principal) {
        User io = userService.findByUsername(principal.getName());

        List<MessageDTO> aggiornati = chatService.segnaLetti(io, request.chatId());
        if (aggiornati.isEmpty()) {
            return;
        }

        consegnaAEntrambi(request.chatId(), aggiornati);
    }

    /**
     * "Sto scrivendo" / "ho smesso".
     *
     * <p>Va solo all'altra persona, non anche a me: vedermi scrivere da solo
     * non aggiunge nulla. E non tocca mai il database — e' un dato che sarebbe
     * gia' vecchio nel momento in cui lo si legge.
     */
    @MessageMapping("/chat.typing")
    public void scrivendo(TypingUpdate richiesta, Principal principal) {
        User io = userService.findByUsername(principal.getName());
        Chat chat = chatService.caricaSePartecipo(richiesta.chatId(), io);

        messagingTemplate.convertAndSendToUser(
                chat.altroRispettoA(io).getUsername(),
                "/queue/typing",
                new TypingUpdate(richiesta.chatId(), io.getUsername(), richiesta.scrivendo()));
    }

    /** Spedisce lo stesso payload ai due partecipanti della conversazione. */
    private void consegnaAEntrambi(UUID chatId, Object payload) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        chat.getParticipants().forEach(p ->
                messagingTemplate.convertAndSendToUser(p.getUsername(), CODA_UTENTE, payload));
    }
}
