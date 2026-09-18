package com.example.demo.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Il corpo JSON della richiesta:
 * {"model": ..., "messages": [...], "reasoning": {"enabled": false}, "max_tokens": N}
 *
 * <p>NON_NULL evita che Jackson spedisca campi valorizzati a null, che alcune
 * API interpretano diversamente da "campo assente".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<WireMessage> messages,
        Reasoning reasoning,
        @JsonProperty("max_tokens") Integer maxTokens) {

    public record Reasoning(boolean enabled) {
    }
}
