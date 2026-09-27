package com.group18.dewecs.exception;

/** Thrown for ground-report review-workflow violations (illegal status transitions, missing action note). */
public class GroundReportValidationException extends RuntimeException {

    public GroundReportValidationException(String message) {
        super(message);
    }
}
