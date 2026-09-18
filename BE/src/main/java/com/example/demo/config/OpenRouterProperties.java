package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tutte le impostazioni di OpenRouter, lette da application.properties sotto il
 * prefisso "openrouter".
 */
@ConfigurationProperties(prefix = "openrouter")
@Getter
@Setter
public class OpenRouterProperties {

    /** Chiave segreta: arriva dalla variabile d'ambiente OPENROUTER_API_KEY. */
    private String apiKey;

    private String baseUrl = "https://openrouter.ai/api/v1";

    private String defaultModel = "nex-agi/nex-n2.5-mini:free";

    /** Etichetta leggibile del modello, per mostrarla nel frontend. */
    private String modelLabel = "Nex AGI: Nex-N2.5-Mini (free)";

    /**
     * Ragionamento esteso del modello.
     *
     * <p>Spento di default: un modello reasoning puo' esaurire i token
     * ragionando <em>senza arrivare a scrivere la risposta finale</em>, e per un
     * suggerimento che deve comparire in due secondi sotto la casella di testo
     * e' un rischio senza contropartita.
     */
    private boolean reasoningEnabled = false;

    /** Quanti messaggi di cronologia mandare al modello a ogni richiesta. */
    private int maxHistoryMessages = 12;

    /** Tetto ai token della risposta: tre frasi brevi non ne richiedono di piu'. */
    private int maxTokens = 300;

    /** Titolo mostrato nella dashboard di OpenRouter. */
    private String appTitle = "Filo Rosso";

    /** Origine dichiarata a OpenRouter (header HTTP-Referer). */
    private String referer = "http://localhost:5173";
}
