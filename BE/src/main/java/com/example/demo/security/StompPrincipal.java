package com.example.demo.security;

import java.security.Principal;

/**
 * Identita' minima associata a una sessione STOMP. Serve a Spring per risolvere
 * le destinazioni che iniziano con /user, cioe' le consegne mirate a un singolo
 * utente invece che in broadcast.
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
