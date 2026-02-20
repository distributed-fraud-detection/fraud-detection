package com.frauddetection.frauddecision.service;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import com.frauddetection.frauddecision.rule.DecisionResult;
import com.frauddetection.frauddecision.rule.DecisionRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the fraud decision pipeline: decide → persist → publish.
 *
 * SOLID Fixes Applied:
 * ─────────────────────────────────────────────────────────────────────────────
 * SRP: Decision logic, persistence, and Kafka publishing were all inline in the
 * Kafka listener. This service extracts the business pipeline; the listener
 * becomes a thin Kafka adapter.
 *
 * OCP: Decision logic is now a Chain of Responsibility via List<DecisionRule>.
 * Spring injects all @Component DecisionRule beans ordered by @Order.
 * Adding ESCALATE: new class + @Order(1.5) equivalent — zero edits here. ✅
 *
 * DIP: Depends on DecisionRule interface, not concrete rule classes.
 *
 * Pattern: Chain of Responsibility (decision rules) + Facade (pipeline)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionService {

    private final List<DecisionRule> decisionRules; // ordered by @Order
    private final FraudCaseRepository fraudCaseRepository;
    private final KafkaTemplate<String, FraudDecisionMadeEvent> kafkaTemplate;

    @Value("${kafka.topics.fraud-decision-made:fraud.decision.made}")
    private String fraudDecisionTopic;

    /**
     * Evaluate a risk-scored event: apply the first matching decision rule,
     * persist the FraudCase, and publish the outcome event.
     */
    public void process(RiskScoredEvent event) {
        // 1. Evaluate rules in priority order (Chain of Responsibility)
        DecisionResult result = decisionRules.stream()
                .filter(rule -> rule.matches(event))
                .findFirst()
                .map(rule -> rule.apply(event))
                .orElseThrow(() -> new IllegalStateException(
                        "No decision rule matched for txnId=" + event.getTransactionId()));

        // 2. Persist FraudCase
        String caseId = UUID.randomUUID().toString();
        FraudCase fraudCase = FraudCase.builder()
                .caseId(caseId)
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .riskScore(event.getRiskScore())
                .decision(result.getDecision())
                .status(result.getStatus())
                .flagReason(result.getFlagReason())
                .build();
        fraudCaseRepository.save(fraudCase);

        // 3. Publish FraudDecisionMadeEvent
        FraudDecisionMadeEvent decisionEvent = FraudDecisionMadeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .caseId(caseId)
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .decision(result.getDecision().name())
                .riskScore(event.getRiskScore())
                .flagReason(result.getFlagReason())
                .decidedAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send(fraudDecisionTopic, event.getUserId(), decisionEvent);

        log.info("FraudDecision: caseId={}, txnId={}, decision={}, score={}",
                caseId, event.getTransactionId(), result.getDecision(), event.getRiskScore());
    }
}
