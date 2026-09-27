package com.group18.dewecs.exception;

/** Thrown for shelter business-rule violations (over-capacity check-in, closed shelter, etc). */
public class ShelterValidationException extends RuntimeException {

    public ShelterValidationException(String message) {
        super(message);
    }
}
