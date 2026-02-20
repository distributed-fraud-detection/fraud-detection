package com.frauddetection.transaction.service;

import com.frauddetection.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Handles per-user transaction rate limiting using Redis.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: Rate-limiting logic lived alongside persistence and Kafka publishing
 * inside TransactionService, violating SRP.
 *
 * AFTER: This class has exactly ONE responsibility: check and enforce the
 * per-minute transaction rate limit for a given user.
 *
 * Algorithm:
 * - Atomically increment a Redis counter keyed by userId
 * - On first increment, set a 60-second TTL (sliding window)
 * - If counter exceeds limit, throw RateLimitExceededException (HTTP 429)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${transaction.rate-limit.max-per-minute:10}")
    private int maxPerMinute;

    private static final String KEY_PREFIX = "user:txn_count:";

    /**
     * Checks and enforces rate limit.
     *
     * @throws RateLimitExceededException if the user has exceeded their quota
     */
    public void checkRateLimit(String userId) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            // First request in this window — set TTL
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        if (count != null && count > maxPerMinute) {
            log.warn("Rate limit exceeded: userId={}, count={}, max={}", userId, count, maxPerMinute);
            throw new RateLimitExceededException(userId, maxPerMinute);
        }

        log.debug("Rate limit check passed: userId={}, count={}/{}", userId, count, maxPerMinute);
    }
}
