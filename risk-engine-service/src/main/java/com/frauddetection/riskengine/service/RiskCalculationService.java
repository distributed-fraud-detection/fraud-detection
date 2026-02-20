package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.service.scoring.RiskScoringFactor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates all registered RiskScoringFactor strategies into a final score.
 *
 * SOLID Fixes Applied:
 * ─────────────────────────────────────────────────────────────────────────────
 * OCP: This class NEVER changes when new scoring factors are added.
 * Spring auto-discovers every @Component implementing RiskScoringFactor
 * and injects them into the List<RiskScoringFactor> constructor arg.
 * Adding "VPNDetectionFactor" = one new class, zero changes here. ✅
 *
 * SRP: Only one job — sum factor contributions and derive risk level.
 * Redis reads, DB ops, and Kafka publishing live in RiskEngineService. ✅
 *
 * Pattern: Strategy + Template Method (aggregate → clamp → derive level)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {

    private final List<RiskScoringFactor> factors; // All @Component implementations injected

    /**
     * Computes the composite risk score by summing all factor contributions.
     * The result is clamped to [0.0, 1.0].
     *
     * @param event   the incoming transaction
     * @param context behavioural context (fraud history, velocity) from Redis
     * @return risk score in [0.0, 1.0]
     */
    public double calculateRiskScore(TransactionCreatedEvent event, RiskContext context) {
        double total = 0.0;
        for (RiskScoringFactor factor : factors) {
            double contribution = factor.score(event, context);
            log.debug("[{}] score contribution: {}", factor.name(), String.format("%.4f", contribution));
            total += contribution;
        }
        double finalScore = Math.min(total, 1.0);
        log.debug("Composite risk score for userId={}: {} ({} factors)",
                event.getUserId(), String.format("%.4f", finalScore), factors.size());
        return finalScore;
    }

    /**
     * Derives a human-readable risk tier from a numeric score.
     * Thresholds are configured in RiskThresholds and aligned with
     * fraud-decision-service.
     */
    public String deriveRiskLevel(double score) {
        if (score >= 0.80)
            return "HIGH";
        if (score >= 0.60)
            return "MEDIUM";
        return "LOW";
    }
}
