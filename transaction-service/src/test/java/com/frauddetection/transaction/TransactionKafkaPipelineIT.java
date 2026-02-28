package com.frauddetection.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.events.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: POST /api/transactions → asserts TransactionCreatedEvent
 * is published to Kafka "transactions.created" topic within 5 seconds.
 *
 * Spring Boot 4: @AutoConfigureMockMvc is in
 * org.springframework.boot.test.autoconfigure.web.servlet (unchanged).
 * Uses H2 (in-memory DB) and EmbeddedKafka — no Docker required.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "transactions.created" }, brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093", "port=9093" })
class TransactionKafkaPipelineIT {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        EmbeddedKafkaBroker embeddedKafkaBroker;
        @Autowired
        ObjectMapper objectMapper;

        @Test
        @DisplayName("POST /api/transactions publishes TransactionCreatedEvent to Kafka within 5s")
        void postTransaction_publishesEventToKafka() throws Exception {
                // Arrange a consumer before posting (prevents race condition)
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-pipeline-group", "true",
                                embeddedKafkaBroker);
                Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                                .createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "transactions.created");

                String body = """
                                {
                                  "userId": "user-kafka-test",
                                  "amount": 200.00,
                                  "location": "Bangalore",
                                  "merchantType": "Grocery"
                                }
                                """;

                // Act
                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isCreated());

                // Assert Kafka event received within 5s
                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "transactions.created",
                                Duration.ofSeconds(5));
                assertThat(record).isNotNull();
                assertThat(record.value()).contains("user-kafka-test");

                consumer.close();
        }
}
