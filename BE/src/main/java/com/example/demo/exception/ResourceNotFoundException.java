package com.example.demo.exception;

/** Risorsa non trovata. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
