package com.frauddetection.common.exception;

/**
 * Thrown when a user exceeds the per-minute transaction rate limit.
 * Maps to HTTP 429 Too Many Requests in GlobalExceptionHandler.
 *
 * This is a typed replacement for the raw RuntimeException that was
 * previously thrown in TransactionService — fixing the OCP violation
 * (handler selects response code by type, not string matching).
 */
public class RateLimitExceededException extends FraudDetectionException {

    private final String userId;
    private final int maxAllowed;

    public RateLimitExceededException(String userId, int maxAllowed) {
        super(String.format(
                "Rate limit exceeded for user '%s'. Maximum %d transactions per minute allowed.",
                userId, maxAllowed));
        this.userId = userId;
        this.maxAllowed = maxAllowed;
    }

    public String getUserId() {
        return userId;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }
}
