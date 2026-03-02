package com.frauddetection.transaction.service;

import com.frauddetection.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
        ReflectionTestUtils.setField(rateLimitService, "maxPerMinute", 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("checkRateLimit: first request sets expiry and passes")
    void checkRateLimit_firstRequest_setsExpiry() {
        when(valueOperations.increment("user:txn_count:u001")).thenReturn(1L);

        rateLimitService.checkRateLimit("u001");

        verify(redisTemplate).expire("user:txn_count:u001", Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("checkRateLimit: count over max throws RateLimitExceededException")
    void checkRateLimit_overLimit_throws() {
        when(valueOperations.increment("user:txn_count:u001")).thenReturn(11L);

        assertThatThrownBy(() -> rateLimitService.checkRateLimit("u001"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("u001");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("checkRateLimit: null counter value is tolerated")
    void checkRateLimit_nullCount_noThrow() {
        when(valueOperations.increment("user:txn_count:u001")).thenReturn(null);

        rateLimitService.checkRateLimit("u001");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }
}
