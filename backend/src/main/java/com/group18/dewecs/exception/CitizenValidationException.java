package com.group18.dewecs.exception;

/** Thrown for citizen identification rule violations (invalid NIC, name or phone). */
public class CitizenValidationException extends RuntimeException {

    public CitizenValidationException(String message) {
        super(message);
    }
}
