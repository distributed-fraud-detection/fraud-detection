package com.frauddetection.frauddecision.rule;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.config.DecisionProperties;
import com.frauddetection.frauddecision.entity.FraudCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rule 2: Flag transactions in the grey zone for human REVIEW.
 * Only evaluated if BlockDecisionRule did not match.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ReviewDecisionRule implements DecisionRule {

    private final DecisionProperties props;

    @Override
    public boolean matches(RiskScoredEvent event) {
        return event.getRiskScore() >= props.getReviewThreshold();
    }

    @Override
    public DecisionResult apply(RiskScoredEvent event) {
        return DecisionResult.builder()
                .decision(FraudCase.Decision.REVIEW)
                .status(FraudCase.CaseStatus.PENDING)
                .flagReason(String.format(
                        "Risk score %.4f requires manual REVIEW (%.2f–%.2f range)",
                        event.getRiskScore(),
                        props.getReviewThreshold(),
                        props.getBlockThreshold()))
                .build();
    }
}
