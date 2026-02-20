package com.frauddetection.common.exception;

/**
 * Thrown when a requested resource is not found (HTTP 404).
 * Services should catch this in a @ControllerAdvice and return a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " not found with id: " + id);
    }
}
