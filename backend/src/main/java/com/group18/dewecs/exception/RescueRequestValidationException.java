package com.group18.dewecs.exception;

/** Thrown for rescue-request business-rule violations (invalid priority, illegal status transitions). */
public class RescueRequestValidationException extends RuntimeException {

    public RescueRequestValidationException(String message) {
        super(message);
    }
}
