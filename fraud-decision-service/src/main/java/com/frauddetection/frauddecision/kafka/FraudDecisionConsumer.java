package com.frauddetection.frauddecision.kafka;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.service.FraudDecisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Thin Kafka adapter — receives RiskScoredEvent and delegates to
 * FraudDecisionService.
 *
 * SOLID Fix: SRP — the consumer's ONLY job is message receipt and delegation.
 * All business logic (decision rules, persistence, publishing) lives in
 * FraudDecisionService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionConsumer {

    private final FraudDecisionService fraudDecisionService;

    @KafkaListener(topics = "${kafka.topics.risk-scored:risk.scored}", groupId = "${spring.kafka.consumer.group-id:fraud-decision-group}", containerFactory = "kafkaListenerContainerFactory")
    public void onRiskScored(RiskScoredEvent event) {
        log.info("FraudDecisionConsumer received: txnId={}, score={}",
                event.getTransactionId(), event.getRiskScore());
        try {
            fraudDecisionService.process(event);
        } catch (Exception e) {
            log.error("Fraud decision failed for txnId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            // Production: send to Dead Letter Queue via Spring Kafka error handler config
        }
    }
}
