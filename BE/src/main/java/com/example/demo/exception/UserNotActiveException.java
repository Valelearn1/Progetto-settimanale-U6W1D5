package com.example.demo.exception;

/** Account non ancora attivato. */
public class UserNotActiveException extends RuntimeException {

    public UserNotActiveException(String message) {
        super(message);
    }
}
