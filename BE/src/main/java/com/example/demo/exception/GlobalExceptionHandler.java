package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce le eccezioni in risposte JSON con lo stato HTTP giusto.
 *
 * <p>Senza questa classe ogni eccezione non gestita diventerebbe un 500 con lo
 * stack trace: il frontend non saprebbe distinguere "email gia' presa" da
 * "database irraggiungibile", e all'utente arriverebbe sempre lo stesso
 * messaggio inutile.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException e) {
        return risposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> conflitto(EmailAlreadyExistsException e) {
        return risposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credenziali(InvalidCredentialsException e) {
        return risposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<Map<String, Object>> nonAttivo(UserNotActiveException e) {
        return risposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** Risorsa esistente ma non tua: per esempio la conversazione di due estranei. */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessoNegato(
            org.springframework.security.access.AccessDeniedException e) {
        return risposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, TokenExpiredException.class})
    public ResponseEntity<Map<String, Object>> token(RuntimeException e) {
        return risposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argomento(IllegalArgumentException e) {
        return risposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * OpenRouter e' un servizio esterno: se non risponde la colpa non e' della
     * richiesta del client, per questo 502 e non 400.
     */
    @ExceptionHandler(OpenRouterException.class)
    public ResponseEntity<Map<String, Object>> openRouter(OpenRouterException e) {
        return risposta(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    /** Errori di @Valid sui DTO: raccoglie tutti i campi sbagliati in un colpo solo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validazione(MethodArgumentNotValidException e) {
        String dettagli = e.getBindingResult().getFieldErrors().stream()
                .map(errore -> errore.getField() + ": " + errore.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return risposta(HttpStatus.BAD_REQUEST, dettagli);
    }

    /**
     * Rete di sicurezza per tutto quello che non abbiamo previsto.
     *
     * <p>Senza questo metodo un errore imprevisto uscirebbe come 500 generato
     * da Spring, e nel log non resterebbe nulla di nostro. E' l'unico punto in
     * cui serve lo stack trace completo: un 5xx e' un guasto, e senza la traccia
     * non si trova.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> imprevista(Exception e) {
        log.error("Errore non gestito: {}", e.getMessage(), e);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Errore interno del server. Riprova tra poco.");
    }

    /**
     * Ogni risposta d'errore passa da qui, quindi qui si logga.
     *
     * <p>Il livello segue lo stato: i 4xx sono colpa della richiesta e vanno a
     * WARN (un 404 non e' un guasto), i 5xx vanno a ERROR. Distinguerli e' cio'
     * che rende il log utile: se tutto fosse ERROR, gli errori veri sarebbero
     * sepolti sotto le password sbagliate.
     */
    private ResponseEntity<Map<String, Object>> risposta(HttpStatus stato, String messaggio) {
        if (stato.is5xxServerError()) {
            log.error("{} - {}", stato.value(), messaggio);
        } else {
            log.warn("{} - {}", stato.value(), messaggio);
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("stato", stato.value());
        corpo.put("messaggio", messaggio);
        corpo.put("istante", Instant.now().toString());
        return ResponseEntity.status(stato).body(corpo);
    }
}
