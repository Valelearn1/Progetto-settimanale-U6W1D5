package com.example.demo.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Risposta di OpenRouter. Teniamo solo i campi che ci servono: i campi ignoti
 * (usage, created, id...) vengono scartati da Jackson senza errori.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id, String model, List<Choice> choices, ApiError error) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(WireMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(String message, Integer code) {
    }
}
