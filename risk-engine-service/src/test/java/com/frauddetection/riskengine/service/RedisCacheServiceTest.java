package com.frauddetection.riskengine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOps;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private ListOperations<String, Object> listOps;

    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        redisCacheService = new RedisCacheService(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
    }

    @Test
    @DisplayName("cacheRiskScore stores score+level and sets TTL")
    void cacheRiskScore_storesValues() {
        redisCacheService.cacheRiskScore("u1", 0.66, "MEDIUM");

        verify(hashOps).put("user:risk:u1", "riskScore", "0.66");
        verify(hashOps).put("user:risk:u1", "riskLevel", "MEDIUM");
        verify(redisTemplate).expire("user:risk:u1", Duration.ofHours(24));
    }

    @Test
    @DisplayName("getCachedRiskScore parses cached value")
    void getCachedRiskScore_parsesValue() {
        when(hashOps.get("user:risk:u1", "riskScore")).thenReturn("0.77");

        Double score = redisCacheService.getCachedRiskScore("u1");

        assertThat(score).isEqualTo(0.77);
    }

    @Test
    @DisplayName("getCachedRiskScore returns null when absent")
    void getCachedRiskScore_absent_returnsNull() {
        when(hashOps.get("user:risk:u1", "riskScore")).thenReturn(null);

        assertThat(redisCacheService.getCachedRiskScore("u1")).isNull();
    }

    @Test
    @DisplayName("getRecentFraudCount defaults to zero when missing")
    void getRecentFraudCount_missing_returnsZero() {
        when(hashOps.get("user:risk:u1", "fraudCount")).thenReturn(null);

        assertThat(redisCacheService.getRecentFraudCount("u1")).isZero();
    }

    @Test
    @DisplayName("incrementFraudCount increments and sets TTL")
    void incrementFraudCount_incrementsAndExpires() {
        redisCacheService.incrementFraudCount("u1");

        verify(hashOps).increment("user:risk:u1", "fraudCount", 1);
        verify(redisTemplate).expire("user:risk:u1", Duration.ofHours(24));
    }

    @Test
    @DisplayName("getRecentTxnCount parses value and defaults to zero")
    void getRecentTxnCount_parseAndDefault() {
        when(valueOps.get("user:txn_count:u1")).thenReturn("9");
        assertThat(redisCacheService.getRecentTxnCount("u1")).isEqualTo(9);

        when(valueOps.get("user:txn_count:u2")).thenReturn(null);
        assertThat(redisCacheService.getRecentTxnCount("u2")).isZero();
    }

    @Test
    @DisplayName("addToHotList pushes, trims and expires")
    void addToHotList_updatesList() {
        redisCacheService.addToHotList("txn-1");

        verify(listOps).leftPush("hot:high-risk-transactions", "txn-1");
        verify(listOps).trim("hot:high-risk-transactions", 0, 99);
        verify(redisTemplate).expire("hot:high-risk-transactions", Duration.ofHours(24));
    }
}
