package com.smarthealth.hms.exception;

/** Thrown when a requested record does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
