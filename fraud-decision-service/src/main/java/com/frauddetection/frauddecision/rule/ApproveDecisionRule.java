package com.frauddetection.frauddecision.rule;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.entity.FraudCase;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rule 3 (fallback): APPROVE all transactions that did not match
 * higher-priority rules.
 * Always matches — acts as the default/catch-all in the chain.
 */
@Component
@Order(3)
public class ApproveDecisionRule implements DecisionRule {

    @Override
    public boolean matches(RiskScoredEvent event) {
        return true; // Fallback — always matches
    }

    @Override
    public DecisionResult apply(RiskScoredEvent event) {
        return DecisionResult.builder()
                .decision(FraudCase.Decision.APPROVE)
                .status(FraudCase.CaseStatus.APPROVED)
                .flagReason(null)
                .build();
    }
}
