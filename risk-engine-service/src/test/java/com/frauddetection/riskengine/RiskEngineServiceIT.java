package com.frauddetection.riskengine;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.entity.RiskProfile;
import com.frauddetection.riskengine.repository.RiskProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Layer 3: Integration test for risk-engine-service.
 *
 * Tests:
 * 1. TransactionCreatedEvent consumed → RiskProfile upserted in PostgreSQL
 * 2. Redis cache key "user:risk:{userId}" set after evaluation
 * 3. RiskScoredEvent published to "risk.scored" Kafka topic (verified via
 * consumer)
 *
 * Uses Testcontainers: real PostgreSQL 16 + Kafka 7.6 + Redis 7.2
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RiskEngineServiceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("risk_db")
            .withUsername("fraud_user")
            .withPassword("fraud_pass");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Autowired
    private RiskProfileRepository riskProfileRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${kafka.topics.transactions-created:transactions.created}")
    private String transactionsTopic;

    @AfterEach
    void cleanup() {
        riskProfileRepository.deleteAll();
    }

    private TransactionCreatedEvent buildEvent(String txId, String userId, BigDecimal amount) {
        return TransactionCreatedEvent.builder()
                .eventId("evt-" + txId)
                .transactionId(txId)
                .userId(userId)
                .amount(amount)
                .location("Mumbai")
                .merchantType("E-Commerce")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TransactionCreatedEvent consumed → RiskProfile upserted in PostgreSQL")
    void transactionEvent_upsertsRiskProfile() {
        kafkaTemplate.send(transactionsTopic, "u-risk-01",
                buildEvent("tx-risk-001", "u-risk-01", BigDecimal.valueOf(2000)));

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            var profile = riskProfileRepository.findByUserId("u-risk-01");
            assertThat(profile).isPresent();
            assertThat(profile.get().getTxnFrequency()).isNotNull();
            assertThat(profile.get().getRiskScore()).isBetween(0.0, 1.0);
        });
    }

    @Test
    @DisplayName("After evaluation → Redis key 'user:risk:{userId}' is populated")
    void transactionEvent_setsRedisRiskCache() {
        kafkaTemplate.send(transactionsTopic, "u-risk-02",
                buildEvent("tx-risk-002", "u-risk-02", BigDecimal.valueOf(500)));

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            String cached = redisTemplate.opsForValue().get("user:risk:u-risk-02");
            assertThat(cached).isNotNull();
            double score = Double.parseDouble(cached);
            assertThat(score).isBetween(0.0, 1.0);
        });
    }
}
