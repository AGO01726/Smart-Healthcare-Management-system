package com.smarthealth.hms.exception;

/** Thrown when an operation violates a business rule (e.g. overlapping appointment). The message is safe to show to users. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
