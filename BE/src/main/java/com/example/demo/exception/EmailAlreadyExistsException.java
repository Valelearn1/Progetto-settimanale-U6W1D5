package com.example.demo.exception;

/** Email o username gia' registrati. */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
