package com.group18.dewecs.exception;

/** Thrown for warning-issuance business-rule violations (invalid state transitions, missing publish-time fields). */
public class WarningValidationException extends RuntimeException {

    public WarningValidationException(String message) {
        super(message);
    }
}
