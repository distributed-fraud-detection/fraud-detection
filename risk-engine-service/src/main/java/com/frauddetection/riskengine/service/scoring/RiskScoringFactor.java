package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.service.RiskContext;

/**
 * Strategy interface for individual risk scoring factors.
 *
 * SOLID Fix: OCP (Open/Closed Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: RiskCalculationService had all scoring logic in one method.
 * Adding a new risk factor (e.g., device fingerprint, VPN detection)
 * meant editing the existing calculateRiskScore() method — violating OCP.
 *
 * AFTER: Each factor is an independent implementation of this interface.
 * RiskCalculationService sums them all up through a List<RiskScoringFactor>.
 * Adding a new factor = new class + Spring @Component. Zero existing code
 * changes.
 *
 * Pattern: Strategy (GoF) + Open/Closed Principle
 */
public interface RiskScoringFactor {

    /**
     * Calculate the partial risk contribution of this specific factor.
     *
     * @param event   the incoming transaction event
     * @param context behavioural context from Redis (fraud history, txn frequency)
     * @return partial score in [0.0, 1.0] — contributions are summed and clamped
     */
    double score(TransactionCreatedEvent event, RiskContext context);

    /**
     * Human-readable factor name, used in debug logging.
     * Example: "AmountFactor", "LocationFactor"
     */
    String name();
}
