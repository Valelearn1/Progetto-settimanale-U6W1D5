package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Su ogni richiesta HTTP legge l'header {@code Authorization: Bearer <token>}
 * e, se il token e' valido, mette l'utente nel SecurityContext.
 *
 * <p>Non blocca mai la richiesta: se il token manca o non e' valido lascia
 * semplicemente il contesto vuoto. A decidere se quella rotta richiede
 * l'autenticazione ci pensa la SecurityFilterChain piu' avanti.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFISSO = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFISSO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String username = jwtService.extractUsername(header.substring(PREFISSO.length()));

            if (username != null) {
                // L'utente viene riletto dal database a ogni richiesta: cosi' un
                // account disattivato smette di funzionare subito, senza aspettare
                // la scadenza del token gia' emesso.
                userRepository.findByUsername(username)
                        .filter(User::isActive)
                        .ifPresent(user -> {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    user, null, List.of());
                            auth.setDetails(new WebAuthenticationDetailsSource()
                                    .buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            }
        }

        filterChain.doFilter(request, response);
    }
}
