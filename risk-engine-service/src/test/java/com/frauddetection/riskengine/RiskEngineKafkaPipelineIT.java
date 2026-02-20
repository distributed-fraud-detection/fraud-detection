package com.frauddetection.riskengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.common.events.TransactionCreatedEvent;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: Publish TransactionCreatedEvent to "transactions.created"
 * → assert RiskScoredEvent appears on "risk.scored" within 8s.
 *
 * Validates the full Risk Engine consumer pipeline.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "transactions.created", "risk.scored" }, brokerProperties = {
                "listeners=PLAINTEXT://localhost:9094", "port=9094" })
class RiskEngineKafkaPipelineIT {

        @Autowired
        EmbeddedKafkaBroker embeddedKafkaBroker;
        @Autowired
        ObjectMapper objectMapper;

        @Test
        @DisplayName("TransactionCreatedEvent → RiskScoredEvent on risk.scored within 8s")
        void consumesTransaction_publishesRiskScore() throws Exception {
                TransactionCreatedEvent input = TransactionCreatedEvent.builder()
                                .transactionId("it-tx-risk-001")
                                .userId("u-it-002")
                                .amount(BigDecimal.valueOf(55000)) // HIGH amount → risk score ≥ 0.35
                                .location("Lagos") // HIGH risk location → adds 0.25
                                .merchantType("Crypto Exchange") // HIGH risk merchant → adds 0.20
                                .timestamp(LocalDateTime.now())
                                .build();

                // Produce event to transactions.created
                Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
                KafkaTemplate<String, String> producer = new KafkaTemplate<>(
                                new DefaultKafkaProducerFactory<>(producerProps));
                producer.send(new ProducerRecord<>("transactions.created", objectMapper.writeValueAsString(input)));

                // Consume from risk.scored
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                                "test-risk-consumer", "true", embeddedKafkaBroker);
                Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                                .createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "risk.scored");

                ConsumerRecord<String, String> scored = KafkaTestUtils.getSingleRecord(consumer, "risk.scored",
                                Duration.ofSeconds(8));
                assertThat(scored).isNotNull();

                RiskScoredEvent event = objectMapper.readValue(scored.value(), RiskScoredEvent.class);
                assertThat(event.getTransactionId()).isEqualTo("it-tx-risk-001");
                // HIGH amount (0.35) + offshore (0.25) + crypto (0.20) = at least 0.5
                assertThat(event.getRiskScore()).isGreaterThanOrEqualTo(0.5);

                consumer.close();
        }
}
