package com.frauddetection.frauddecision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: Publish RiskScoredEvent with score 0.90 to "risk.scored"
 * → assert FraudDecisionMadeEvent with decision=BLOCK on "fraud.decision.made"
 * → assert FraudCase row is persisted in H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "risk.scored", "fraud.decision.made" }, brokerProperties = {
                "listeners=PLAINTEXT://localhost:9095", "port=9095" })
class FraudDecisionPipelineIT {

        @Autowired
        EmbeddedKafkaBroker embeddedKafkaBroker;
        @Autowired
        FraudCaseRepository fraudCaseRepository;
        @Autowired
        ObjectMapper objectMapper;

        @Test
        @DisplayName("RiskScoredEvent (0.90) → FraudDecisionMadeEvent BLOCK + FraudCase persisted")
        void highRisk_decidesBlock_andPersists() throws Exception {
                RiskScoredEvent input = RiskScoredEvent.builder()
                                .transactionId("it-tx-decision-001")
                                .userId("u-it-003")
                                .riskScore(0.90)
                                .riskLevel("HIGH")
                                .scoredAt(LocalDateTime.now())
                                .build();

                // Produce to risk.scored
                Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
                KafkaTemplate<String, String> producer = new KafkaTemplate<>(
                                new DefaultKafkaProducerFactory<>(producerProps));
                producer.send(new ProducerRecord<>("risk.scored", objectMapper.writeValueAsString(input)));

                // Listen for decision
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                                "test-decision-consumer", "true", embeddedKafkaBroker);
                Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                                .createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "fraud.decision.made");

                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "fraud.decision.made",
                                Duration.ofSeconds(8));
                assertThat(record).isNotNull();

                FraudDecisionMadeEvent decision = objectMapper.readValue(record.value(), FraudDecisionMadeEvent.class);
                assertThat(decision.getDecision()).isEqualTo("BLOCK");
                assertThat(decision.getTransactionId()).isEqualTo("it-tx-decision-001");

                // Verify FraudCase was persisted
                var fraudCase = fraudCaseRepository.findByTransactionId("it-tx-decision-001");
                assertThat(fraudCase).isPresent();
                assertThat(fraudCase.get().getDecision().name()).isEqualTo("BLOCK");

                consumer.close();
        }

        @Test
        @DisplayName("RiskScoredEvent (0.70) → REVIEW decision")
        void mediumRisk_decidesReview() throws Exception {
                RiskScoredEvent input = RiskScoredEvent.builder()
                                .transactionId("it-tx-decision-002")
                                .userId("u-it-004")
                                .riskScore(0.70)
                                .riskLevel("MEDIUM")
                                .scoredAt(LocalDateTime.now())
                                .build();

                Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
                KafkaTemplate<String, String> producer = new KafkaTemplate<>(
                                new DefaultKafkaProducerFactory<>(producerProps));
                producer.send(new ProducerRecord<>("risk.scored", objectMapper.writeValueAsString(input)));

                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                                "test-review-consumer", "true", embeddedKafkaBroker);
                Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                                .createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "fraud.decision.made");

                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "fraud.decision.made",
                                Duration.ofSeconds(8));
                FraudDecisionMadeEvent decision = objectMapper.readValue(record.value(), FraudDecisionMadeEvent.class);
                assertThat(decision.getDecision()).isEqualTo("REVIEW");

                consumer.close();
        }
}
