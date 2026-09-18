package com.example.demo.dto.response;

import com.example.demo.entity.Message;

import java.time.Instant;
import java.util.UUID;

/**
 * Un messaggio come lo vede il frontend. Il mittente e' appiattito nei suoi
 * quattro campi utili invece di annidare un UserDTO: la lista messaggi e' la
 * struttura piu' ripetuta dell'applicazione, e tenerla piatta evita al client
 * di dover risalire l'albero a ogni bolla da disegnare.
 */
public record MessageDTO(
        Long id,
        UUID chatId,
        UUID senderId,
        String senderUsername,
        String senderDisplayName,
        String senderAvatarColor,
        String content,
        Instant sentAt,
        Instant readAt) {

    public static MessageDTO from(Message m) {
        return new MessageDTO(
                m.getId(),
                m.getChat().getId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.getSender().getDisplayName(),
                m.getSender().getAvatarColor(),
                m.getContent(),
                m.getSentAt(),
                m.getReadAt());
    }
}
