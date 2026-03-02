package com.frauddetection.transaction.service;

public interface RateLimitService {

    void checkRateLimit(String userId);
}
