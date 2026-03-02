package com.frauddetection.frauddecision.service.impl;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import com.frauddetection.frauddecision.rule.DecisionResult;
import com.frauddetection.frauddecision.rule.DecisionRule;
import com.frauddetection.frauddecision.service.FraudDecisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionServiceImpl implements FraudDecisionService {

    private final List<DecisionRule> decisionRules;
    private final FraudCaseRepository fraudCaseRepository;
    private final KafkaTemplate<String, FraudDecisionMadeEvent> kafkaTemplate;

    @Value("${kafka.topics.fraud-decision-made:fraud.decision.made}")
    private String fraudDecisionTopic;

    @Override
    public void process(RiskScoredEvent event) {
        DecisionResult result = decisionRules.stream()
                .filter(rule -> rule.matches(event))
                .findFirst()
                .map(rule -> rule.apply(event))
                .orElseThrow(() -> new IllegalStateException(
                        "No decision rule matched for txnId=" + event.getTransactionId()));

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
