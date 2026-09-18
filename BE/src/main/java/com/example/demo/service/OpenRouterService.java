package com.example.demo.service;

import com.example.demo.config.OpenRouterProperties;
import com.example.demo.dto.openrouter.ChatCompletionRequest;
import com.example.demo.dto.openrouter.ChatCompletionResponse;
import com.example.demo.dto.openrouter.WireMessage;
import com.example.demo.exception.OpenRouterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Traduzione in Java del comando cURL di OpenRouter.
 *
 * <pre>
 * curl https://openrouter.ai/api/v1/chat/completions   -> baseUrl + uri()
 *   -H "Authorization: Bearer $KEY"                    -> header(...)
 *   -H "Content-Type: application/json"                -> defaultHeader nel RestClient
 *   -d '{"model": ..., "messages": [...]}'             -> body(ChatCompletionRequest)
 * </pre>
 */
@Service
@Slf4j
public class OpenRouterService {

    private final RestClient client;
    private final OpenRouterProperties properties;

    public OpenRouterService(RestClient openRouterClient, OpenRouterProperties properties) {
        this.client = openRouterClient;
        this.properties = properties;
    }

    /**
     * Manda la conversazione al modello e restituisce il testo della risposta.
     * La finestra di contesto va rispedita per intero a ogni giro: l'API e'
     * senza memoria.
     */
    public String completa(List<WireMessage> conversazione) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenRouterException(
                    "Manca la API key: imposta OPENROUTER_API_KEY in BE/.env "
                            + "prima di avviare il backend.");
        }

        ChatCompletionRequest richiesta = new ChatCompletionRequest(
                properties.getDefaultModel(),
                conversazione,
                new ChatCompletionRequest.Reasoning(properties.isReasoningEnabled()),
                properties.getMaxTokens());

        // La durata e' il dato piu' utile da avere qui: OpenRouter e' l'unica
        // dipendenza esterna, ed e' quella che puo' rallentare tutto il resto.
        long inizio = System.nanoTime();

        ChatCompletionResponse risposta;
        try {
            risposta = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(richiesta)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String corpo = new String(
                                res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new OpenRouterException(
                                "OpenRouter ha risposto " + res.getStatusCode() + " - " + corpo);
                    })
                    .body(ChatCompletionResponse.class);
        } catch (ResourceAccessException e) {
            // Host irraggiungibile o timeout: e' diverso da "ha risposto male",
            // e all'utente va detto in modo diverso.
            log.error("OpenRouter irraggiungibile dopo {} ms: {}", millisDa(inizio), e.getMessage());
            throw new OpenRouterException(
                    "Impossibile contattare OpenRouter: " + e.getMessage(), e);
        }

        String contenuto = estraiContenuto(risposta);
        // La chiave non compare mai: si logga il modello, non le credenziali.
        log.info("OpenRouter: modello={} reasoning={} durata={} ms messaggi={}",
                properties.getDefaultModel(),
                properties.isReasoningEnabled(),
                millisDa(inizio),
                conversazione.size());
        return contenuto;
    }

    private static long millisDa(long inizioNano) {
        return (System.nanoTime() - inizioNano) / 1_000_000;
    }

    private String estraiContenuto(ChatCompletionResponse risposta) {
        if (risposta == null) {
            throw new OpenRouterException("OpenRouter ha restituito una risposta vuota.");
        }
        if (risposta.error() != null) {
            throw new OpenRouterException("OpenRouter: " + risposta.error().message());
        }
        if (risposta.choices() == null || risposta.choices().isEmpty()) {
            throw new OpenRouterException("OpenRouter non ha restituito nessuna risposta.");
        }

        WireMessage messaggio = risposta.choices().get(0).message();
        if (messaggio == null || messaggio.content() == null || messaggio.content().isBlank()) {
            // Capita coi modelli reasoning che esauriscono i token ragionando
            // senza arrivare a scrivere la risposta finale.
            throw new OpenRouterException(
                    "Il modello ha risposto senza contenuto testuale. "
                            + "Riprova, oppure disattiva openrouter.reasoning-enabled.");
        }

        return messaggio.content().trim();
    }
}
