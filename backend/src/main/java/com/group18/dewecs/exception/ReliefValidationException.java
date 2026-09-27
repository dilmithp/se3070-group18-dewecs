package com.group18.dewecs.exception;

/** Thrown for relief-supply/distribution business-rule violations (stock, status transitions). */
public class ReliefValidationException extends RuntimeException {

    public ReliefValidationException(String message) {
        super(message);
    }
}
