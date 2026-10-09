package com.group18.dewecs.exception;

/** Thrown for hazard-event business-rule violations (invalid type or severity, a start time in the future, going back a status). */
public class HazardEventValidationException extends RuntimeException {

    public HazardEventValidationException(String message) {
        super(message);
    }
}
