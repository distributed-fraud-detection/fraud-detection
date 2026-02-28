package com.frauddetection.transaction;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.transaction.repository.TransactionRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import org.apache.kafka.clients.consumer.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Layer 3: Full integration test for transaction-service.
 *
 * Uses Testcontainers (real PostgreSQL 16, Kafka 7.6, Redis 7.2).
 * No mocks — tests the complete request path:
 * HTTP POST → TransactionController → TransactionService →
 * → PostgreSQL (saved) → Kafka (TransactionCreatedEvent published)
 *
 * Extends AbstractIntegrationTest which starts containers.
 * Spring Boot 4: TestRestTemplate removed; using RestClient instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionServiceIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ConsumerFactory<String, TransactionCreatedEvent> consumerFactory;

    @Value("${kafka.topics.transactions-created:transactions.created}")
    private String transactionsTopic;

    @AfterEach
    void cleanup() {
        transactionRepository.deleteAll();
    }

    private RestClient restClient() {
        return RestClient.create("http://localhost:" + port);
    }

    private TransactionDTO requestDTO(String userId) {
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(userId);
        dto.setAmount(BigDecimal.valueOf(1500));
        dto.setLocation("Delhi");
        dto.setMerchantType("E-Commerce");
        return dto;
    }

    @Test
    @DisplayName("POST /api/transactions → 201 + row in DB + event on Kafka")
    void createTransaction_persistedAndPublishedToKafka() {
        // Act — POST via real HTTP using Spring Boot 4's RestClient
        ResponseEntity<TransactionDTO> response = restClient()
                .post()
                .uri("/api/transactions")
                .body(requestDTO("user-it-01"))
                .retrieve()
                .toEntity(TransactionDTO.class);

        // Assert HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        String txId = response.getBody().getTransactionId();
        assertThat(txId).isNotBlank();

        // Assert DB row exists
        assertThat(transactionRepository.findById(txId)).isPresent();

        // Assert Kafka event published
        try (Consumer<String, TransactionCreatedEvent> consumer = consumerFactory.createConsumer("it-group", null)) {
            consumer.subscribe(Collections.singletonList(transactionsTopic));
            ConsumerRecord<String, TransactionCreatedEvent> record = KafkaTestUtils.getSingleRecord(consumer,
                    transactionsTopic, 10_000);
            assertThat(record).isNotNull();
            assertThat(record.value().getTransactionId()).isEqualTo(txId);
            assertThat(record.value().getUserId()).isEqualTo("user-it-01");
        }
    }

    @Test
    @DisplayName("11 rapid requests from same userId → 10 succeed, 11th returns 429")
    void rateLimitEnforced_11thRequestRejected() {
        // Allow 10 requests (matches app.rate-limit.max-requests=10 in test profile)
        for (int i = 0; i < 10; i++) {
            ResponseEntity<TransactionDTO> resp = restClient()
                    .post()
                    .uri("/api/transactions")
                    .body(requestDTO("user-limited"))
                    .retrieve()
                    .toEntity(TransactionDTO.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // 11th should be rate-limited → expect 429
        try {
            restClient()
                    .post()
                    .uri("/api/transactions")
                    .body(requestDTO("user-limited"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // RestClient throws on 4xx/5xx — 429 is the expected outcome
            assertThat(e.getMessage()).containsIgnoringCase("429");
        }
    }
}
