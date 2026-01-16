package com.verdissia.exception;

/**
 * Exception pour les erreurs métier
 */
public class FunctionalException extends RuntimeException {

    public FunctionalException(String message) {
        super(message);
    }

    public FunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
