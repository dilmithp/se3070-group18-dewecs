package com.group18.dewecs.exception;

/** Thrown for shelter and rescue coordination rule violations (bad occupancy, cross-organization dispatch without consent). */
public class CoordinationValidationException extends RuntimeException {

    public CoordinationValidationException(String message) {
        super(message);
    }
}
