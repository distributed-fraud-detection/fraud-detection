package com.frauddetection.riskengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RISK_KEY_PREFIX        = "user:risk:";
    private static final String TXN_COUNT_KEY_PREFIX   = "user:txn_count:";
    private static final String HOT_HIGH_RISK_LIST_KEY = "hot:high-risk-transactions";
    private static final int    HIGH_RISK_LIST_SIZE    = 100;

    // ── Risk profile cache ─────────────────────────────────────────────────
    public void cacheRiskScore(String userId, double riskScore, String riskLevel) {
        String key = RISK_KEY_PREFIX + userId;
        redisTemplate.opsForHash().put(key, "riskScore", String.valueOf(riskScore));
        redisTemplate.opsForHash().put(key, "riskLevel", riskLevel);
        redisTemplate.expire(key, Duration.ofHours(24));
        log.debug("Cached risk profile for user={}: score={}", userId, riskScore);
    }

    public Double getCachedRiskScore(String userId) {
        Object score = redisTemplate.opsForHash().get(RISK_KEY_PREFIX + userId, "riskScore");
        return score != null ? Double.parseDouble(score.toString()) : null;
    }

    public int getRecentFraudCount(String userId) {
        Object count = redisTemplate.opsForHash().get(RISK_KEY_PREFIX + userId, "fraudCount");
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }

    public void incrementFraudCount(String userId) {
        String key = RISK_KEY_PREFIX + userId;
        redisTemplate.opsForHash().increment(key, "fraudCount", 1);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    // ── Transaction frequency (rate-limit window) ─────────────────────────
    public int getRecentTxnCount(String userId) {
        Object count = redisTemplate.opsForValue().get(TXN_COUNT_KEY_PREFIX + userId);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }

    // ── Hot high-risk transaction list (latest 100) ───────────────────────
    public void addToHotList(String transactionId) {
        redisTemplate.opsForList().leftPush(HOT_HIGH_RISK_LIST_KEY, transactionId);
        redisTemplate.opsForList().trim(HOT_HIGH_RISK_LIST_KEY, 0, HIGH_RISK_LIST_SIZE - 1);
        redisTemplate.expire(HOT_HIGH_RISK_LIST_KEY, Duration.ofHours(24));
    }
}
