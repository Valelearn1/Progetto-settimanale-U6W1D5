package com.example.demo.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un messaggio nel formato che si aspetta OpenRouter:
 * {"role": "...", "content": "..."}
 *
 * <p>In risposta il modello puo' aggiungere campi suoi (reasoning, refusal...):
 * vengono scartati invece di far fallire la deserializzazione.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WireMessage(String role, String content) {

    /** Le istruzioni al modello: chi e' e cosa deve produrre. */
    public static WireMessage system(String content) {
        return new WireMessage("system", content);
    }

    /** Nel nostro uso: cio' che ha detto l'interlocutore. */
    public static WireMessage user(String content) {
        return new WireMessage("user", content);
    }

    /** Nel nostro uso: cio' che ho detto io, di cui il modello deve prendere il posto. */
    public static WireMessage assistant(String content) {
        return new WireMessage("assistant", content);
    }
}
