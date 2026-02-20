package com.frauddetection.riskengine.kafka;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener — thin adapter that delegates to RiskEngineService.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: This class was a God class with cache reads, scoring, DB writes,
 * Redis writes, and Kafka publishing all inline — 85 lines of mixed concerns.
 *
 * AFTER: Single responsibility = "receive Kafka message → delegate → done."
 * Error handling stays here (infrastructure concern), business logic is gone.
 *
 * This separation means:
 * - RiskEngineService can be unit-tested without any Kafka machinery
 * - The Kafka consumer can be changed (e.g., to batch listening) without
 * touching any business logic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskEngineConsumer {

    private final RiskEngineService riskEngineService;

    @KafkaListener(topics = "${kafka.topics.transactions-created:transactions.created}", groupId = "${spring.kafka.consumer.group-id:risk-engine-group}", containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("RiskEngineConsumer received: txnId={}, userId={}",
                event.getTransactionId(), event.getUserId());
        try {
            riskEngineService.evaluate(event);
        } catch (Exception e) {
            log.error("Risk evaluation failed for txnId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            // In production: push to DLQ (Dead Letter Queue) via Spring Kafka error handler
        }
    }
}
