package com.Spring_chat.Spring_chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic application exception that carries an HTTP status code.
 * Handled globally by {@link GlobalExceptionHandler}.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
