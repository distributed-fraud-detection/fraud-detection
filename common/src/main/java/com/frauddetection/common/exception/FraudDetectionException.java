package com.frauddetection.common.exception;

/**
 * General domain exception for the fraud detection platform.
 * Services should catch this in a @ControllerAdvice and return an appropriate
 * HTTP response.
 */
public class FraudDetectionException extends RuntimeException {

    private final String errorCode;

    public FraudDetectionException(String message) {
        super(message);
        this.errorCode = "FRAUD_DETECTION_ERROR";
    }

    public FraudDetectionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FraudDetectionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
