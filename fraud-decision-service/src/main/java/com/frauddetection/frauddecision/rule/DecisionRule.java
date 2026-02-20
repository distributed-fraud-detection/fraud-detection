package com.frauddetection.frauddecision.rule;

import com.frauddetection.common.events.RiskScoredEvent;

/**
 * Chain-of-Responsibility / Strategy interface for fraud decision rules.
 *
 * SOLID Fix: OCP (Open/Closed Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: FraudDecisionConsumer had an if/else if/else chain with hardcoded
 * thresholds (0.8, 0.6). Adding an ESCALATE tier for VIP customers
 * meant editing the inline consumer method — classic OCP violation.
 *
 * AFTER: Each decision tier is a separate DecisionRule implementation.
 * FraudDecisionService iterates the ordered rule list and calls the
 * first matching rule. Adding a new "ESCALATE" rule = new class,
 * zero changes to FraudDecisionService. ✅
 *
 * Rules are ordered by priority (highest risk first) — Spring's @Order
 * annotation on each implementation controls the evaluation sequence.
 */
public interface DecisionRule {

    /**
     * Returns true if this rule applies to the given event.
     * Rules are evaluated in priority order; the first match wins.
     */
    boolean matches(RiskScoredEvent event);

    /**
     * Builds and returns the decision result for a matching event.
     * Only called after matches() returns true.
     */
    DecisionResult apply(RiskScoredEvent event);
}
