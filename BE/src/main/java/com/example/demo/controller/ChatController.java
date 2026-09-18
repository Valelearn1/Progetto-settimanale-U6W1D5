package com.example.demo.controller;

import com.example.demo.dto.request.CreateChatRequestDTO;
import com.example.demo.dto.response.ChatSummaryDTO;
import com.example.demo.dto.response.MessageDTO;
import com.example.demo.entity.Chat;
import com.example.demo.entity.User;
import com.example.demo.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * La cronologia e l'elenco delle conversazioni viaggiano su HTTP, non sul
 * WebSocket: sono domande con una risposta sola, fatte all'apertura
 * dell'applicazione. Il socket serve a cio' che arriva <em>dopo</em>.
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** La sidebar completa: una riga per conversazione. */
    @GetMapping
    public List<ChatSummaryDTO> mie(@AuthenticationPrincipal User io) {
        return chatService.sidebar(io);
    }

    /**
     * Apre la conversazione con una persona. Se esiste gia' restituisce quella,
     * senza crearne una seconda: e' il "clicca su un contatto" di Discord.
     */
    @PostMapping
    public ResponseEntity<ChatSummaryDTO> apri(
            @AuthenticationPrincipal User io,
            @Valid @RequestBody CreateChatRequestDTO dto) {
        Chat chat = chatService.trovaOCrea(io, dto.peerId());
        return ResponseEntity.ok(chatService.riassumi(chat, io));
    }

    /**
     * Mette o toglie la conversazione dai preferiti.
     *
     * <p>PUT e non POST: l'operazione e' un cambio di stato su una risorsa che
     * esiste gia', e chiamarla due volte di fila riporta al punto di partenza
     * in modo prevedibile.
     */
    @PutMapping("/{chatId}/favorite")
    public ChatSummaryDTO preferita(
            @AuthenticationPrincipal User io,
            @PathVariable UUID chatId) {
        return chatService.cambiaPreferita(io, chatId);
    }

    @GetMapping("/{chatId}/messages")
    public List<MessageDTO> messaggi(
            @AuthenticationPrincipal User io,
            @PathVariable UUID chatId) {
        return chatService.cronologia(io, chatId);
    }
}
