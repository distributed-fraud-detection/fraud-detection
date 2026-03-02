package com.frauddetection.transaction.service.impl;

import com.frauddetection.common.exception.RateLimitExceededException;
import com.frauddetection.transaction.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${transaction.rate-limit.max-per-minute:10}")
    private int maxPerMinute;

    private static final String KEY_PREFIX = "user:txn_count:";

    @Override
    public void checkRateLimit(String userId) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        if (count != null && count > maxPerMinute) {
            log.warn("Rate limit exceeded: userId={}, count={}, max={}", userId, count, maxPerMinute);
            throw new RateLimitExceededException(userId, maxPerMinute);
        }

        log.debug("Rate limit check passed: userId={}, count={}/{}", userId, count, maxPerMinute);
    }
}
