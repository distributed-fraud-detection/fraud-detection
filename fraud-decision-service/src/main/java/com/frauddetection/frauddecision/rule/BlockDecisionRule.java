package com.frauddetection.frauddecision.rule;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.config.DecisionProperties;
import com.frauddetection.frauddecision.entity.FraudCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rule 1 (highest priority): BLOCK transactions above the block threshold.
 * Evaluated first — if score > blockThreshold, no further rules are checked.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class BlockDecisionRule implements DecisionRule {

    private final DecisionProperties props;

    @Override
    public boolean matches(RiskScoredEvent event) {
        return event.getRiskScore() > props.getBlockThreshold();
    }

    @Override
    public DecisionResult apply(RiskScoredEvent event) {
        return DecisionResult.builder()
                .decision(FraudCase.Decision.BLOCK)
                .status(FraudCase.CaseStatus.BLOCKED)
                .flagReason(String.format(
                        "Risk score %.4f exceeds BLOCK threshold (%.2f)",
                        event.getRiskScore(), props.getBlockThreshold()))
                .build();
    }
}
