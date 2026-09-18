package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** "Proponimi una risposta per questa conversazione." */
public record SuggestionRequestDTO(@NotNull(message = "chatId è obbligatorio") UUID chatId) {
}
