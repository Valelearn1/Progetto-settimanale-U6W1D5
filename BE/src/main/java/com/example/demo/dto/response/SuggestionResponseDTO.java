package com.example.demo.dto.response;

import java.util.List;

/**
 * Le proposte dell'AI. Vivono solo dentro questa risposta HTTP: non esiste
 * nessuna tabella che le conservi (requisito R4).
 *
 * <p>Il modello viene restituito insieme alle proposte per poterlo mostrare
 * nell'interfaccia: chi legge un suggerimento ha diritto di sapere da dove
 * arriva.
 */
public record SuggestionResponseDTO(List<String> proposte, String modello) {
}
