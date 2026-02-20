package com.frauddetection.frauddecision.rule;

import com.frauddetection.frauddecision.entity.FraudCase;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable Value Object carrying the outcome of a DecisionRule evaluation.
 *
 * Pattern: Value Object (DDD) — carries decision data without behaviour.
 * All fields are final; Lombok @Value ensures full immutability.
 */
@Value
@Builder
public class DecisionResult {

    FraudCase.Decision decision;
    FraudCase.CaseStatus status;
    String flagReason;
}
