package io.github.smling.proxmoxmcpserver.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps common exceptions to HTTP responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    /**
     * Handles bad request errors surfaced as illegal arguments.
     *
     * @param ex the exception
     * @return the HTTP 400 response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles unexpected exceptions as internal server errors.
     *
     * @param ex the exception
     * @return the HTTP 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleServerError(Exception ex) {
        logger.error("Unhandled server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
