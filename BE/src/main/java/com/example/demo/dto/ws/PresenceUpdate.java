package com.example.demo.dto.ws;

/**
 * Stato di presenza di un utente. Viaggia in broadcast su /topic/presence:
 * chi e' online e' informazione pubblica fra gli iscritti, non un dato mirato.
 */
public record PresenceUpdate(String username, boolean online) {
}
