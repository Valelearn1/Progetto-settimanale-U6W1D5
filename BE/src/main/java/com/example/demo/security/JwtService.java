package com.example.demo.security;

import com.example.demo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Crea e verifica i token JWT.
 *
 * <p>Un JWT e' composto da tre parti separate da punto: header, payload e
 * firma. Il payload NON e' cifrato, solo codificato in base64: chiunque puo'
 * leggerlo. Quello che nessuno puo' fare senza il segreto e' <em>falsificare la
 * firma</em>, ed e' questo che rende il token affidabile. Quindi dentro non si
 * mettono mai dati sensibili.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes) {

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret deve essere lungo almeno 32 caratteri "
                            + "(richiesto da HMAC-SHA256).");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Genera il token per un utente appena autenticato.
     *
     * <p>Il subject e' lo username perche' e' l'identita' usata sia dalle REST
     * sia dalle destinazioni /user del WebSocket. L'id viaggia come claim a
     * parte per evitare una query in piu' dove serve solo quello.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Restituisce lo username contenuto nel token, oppure null se il token e'
     * scaduto, manomesso o illeggibile. Il null al posto dell'eccezione tiene
     * semplice il chiamante: un token non valido e' semplicemente "nessuno".
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
