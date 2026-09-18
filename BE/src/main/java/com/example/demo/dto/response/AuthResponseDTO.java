package com.example.demo.dto.response;

/**
 * Risposta del login: il token da mettere nell'header Authorization e i dati
 * dell'utente, cosi' il frontend puo' mostrare subito nome e avatar senza una
 * seconda chiamata.
 */
public record AuthResponseDTO(String token, UserDTO utente) {
}
