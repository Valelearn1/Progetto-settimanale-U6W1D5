package com.example.demo.exception;

/** Codice scaduto. */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
