package com.frauddetection.notification.kafka;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Thin Kafka adapter — receives FraudDecisionMadeEvent and delegates to
 * NotificationService.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: This class combined Kafka message receipt, notification entity
 * construction, message building, and repository persistence in one method —
 * four responsibilities in a single Kafka listener.
 *
 * AFTER: Single responsibility = "receive Kafka message → delegate → handle
 * errors."
 * All business logic (message building, persistence) lives in
 * NotificationService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topics.fraud-decision-made:fraud.decision.made}", groupId = "${spring.kafka.consumer.group-id:notification-group}", containerFactory = "kafkaListenerContainerFactory")
    public void onFraudDecisionMade(FraudDecisionMadeEvent event) {
        log.info("NotificationConsumer received: caseId={}, decision={}",
                event.getCaseId(), event.getDecision());
        try {
            notificationService.processDecision(event);
        } catch (Exception e) {
            log.error("Failed to process FraudDecisionMadeEvent: caseId={}: {}",
                    event.getCaseId(), e.getMessage(), e);
            // Production: route to Dead Letter Queue via Spring Kafka error handler
        }
    }
}
