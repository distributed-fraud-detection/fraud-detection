package com.frauddetection.frauddecision;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Layer 3: Integration test for fraud-decision-service.
 *
 * Tests:
 * 1. Kafka consumer: RiskScoredEvent(score=0.9) → FraudCase(decision=BLOCK)
 * saved in DB
 * 2. Kafka consumer: RiskScoredEvent(score=0.7) → FraudCase(decision=REVIEW)
 * 3. REST review API: PUT /api/fraud-cases/{id}/review APPROVE → status updated
 *
 * Uses Awaitility for async Kafka consumption assertions (standard pattern).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FraudDecisionServiceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("fraud_db")
            .withUsername("fraud_user")
            .withPassword("fraud_pass");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, RiskScoredEvent> kafkaTemplate;

    @Autowired
    private FraudCaseRepository fraudCaseRepository;

    @LocalServerPort
    private int port;

    @Value("${kafka.topics.risk-scored:risk.scored}")
    private String riskScoredTopic;

    @AfterEach
    void cleanup() {
        fraudCaseRepository.deleteAll();
    }

    private RiskScoredEvent buildRiskEvent(String txId, double score) {
        String level = score > 0.8 ? "HIGH" : score > 0.6 ? "MEDIUM" : "LOW";
        return RiskScoredEvent.builder()
                .eventId("evt-" + txId)
                .transactionId(txId)
                .userId("u-it-01")
                .riskScore(score)
                .riskLevel(level)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("RiskScoredEvent(score=0.9) → FraudCase persisted with decision=BLOCK")
    void highRiskEvent_createsBlockedFraudCase() {
        kafkaTemplate.send(riskScoredTopic, "u-it-01", buildRiskEvent("tx-block-01", 0.92));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // findByTransactionId returns Optional<FraudCase>, not List
            FraudCase fraudCase = fraudCaseRepository.findByTransactionId("tx-block-01").orElseThrow();
            assertThat(fraudCase.getDecision().name()).isEqualTo("BLOCK");
        });
    }

    @Test
    @DisplayName("RiskScoredEvent(score=0.7) → FraudCase persisted with decision=REVIEW")
    void mediumRiskEvent_createsReviewFraudCase() {
        kafkaTemplate.send(riskScoredTopic, "u-it-01", buildRiskEvent("tx-review-01", 0.73));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // findByTransactionId returns Optional<FraudCase>, not List
            FraudCase fraudCase = fraudCaseRepository.findByTransactionId("tx-review-01").orElseThrow();
            assertThat(fraudCase.getDecision().name()).isEqualTo("REVIEW");
        });
    }

    @Test
    @DisplayName("PUT /api/fraud-cases/{id}/review APPROVE → case status updated in DB")
    void reviewCase_approve_updatesDatabase() {
        // Seed a REVIEW case directly
        FraudCase pending = FraudCase.builder()
                .transactionId("tx-pending-99")
                .userId("u-it-01")
                .riskScore(0.71)
                .decision(FraudCase.Decision.REVIEW)
                .build();
        FraudCase saved = fraudCaseRepository.save(pending);

        // Call the review endpoint via RestClient (Spring Boot 4; TestRestTemplate
        // removed)
        RestClient restClient = RestClient.create("http://localhost:" + port);
        ResponseEntity<String> response = restClient.put()
                .uri("/api/fraud-cases/" + saved.getCaseId() + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("action", "APPROVE"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify in DB — findByCaseId(String) uses business key, not surrogate Long PK
        FraudCase updated = fraudCaseRepository.findByCaseId(saved.getCaseId()).orElseThrow();
        assertThat(updated.getDecision().name()).isEqualTo("APPROVE");
    }
}
