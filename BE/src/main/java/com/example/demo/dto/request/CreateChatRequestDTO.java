package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Apre (o ritrova) la conversazione con questa persona. */
public record CreateChatRequestDTO(@NotNull(message = "peerId è obbligatorio") UUID peerId) {
}
