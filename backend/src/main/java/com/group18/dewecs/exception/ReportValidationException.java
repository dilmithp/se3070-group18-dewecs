package com.group18.dewecs.exception;

/** Thrown for post-event report rule violations (an override without a justification, approving a provisional draft). */
public class ReportValidationException extends RuntimeException {

    public ReportValidationException(String message) {
        super(message);
    }
}
