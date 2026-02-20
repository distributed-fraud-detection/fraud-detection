package com.frauddetection.riskengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized risk threshold configuration.
 *
 * SOLID Fix: OCP (Open/Closed Principle) + Configurable Magic Numbers
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: All thresholds (10000, 5000, 0.8, 0.6, 0.35, 0.25 ...) were
 * magic numbers hardcoded inside RiskCalculationService and
 * FraudDecisionConsumer.
 *
 * AFTER: Every threshold lives in application.yml under the `risk:` prefix.
 * DevOps/SRE can tune scoring without touching or redeploying code.
 *
 * Usage in application.yml:
 * 
 * <pre>
 * risk:
 *   high-amount-threshold: 10000
 *   medium-amount-threshold: 5000
 *   block-threshold: 0.8
 *   review-threshold: 0.6
 *   amount-high-score: 0.35
 *   amount-medium-score: 0.20
 *   amount-low-score: 0.05
 *   location-high-score: 0.25
 *   location-low-score: 0.05
 *   merchant-high-score: 0.20
 *   merchant-low-score: 0.05
 *   frequency-high-threshold: 8
 *   frequency-medium-threshold: 5
 *   frequency-high-score: 0.15
 *   frequency-medium-score: 0.10
 *   fraud-count-multiplier: 0.05
 *   fraud-count-max-bonus: 0.20
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "risk")
@Data
public class RiskThresholds {

    // Amount scoring
    private double highAmountThreshold = 10_000;
    private double mediumAmountThreshold = 5_000;
    private double amountHighScore = 0.35;
    private double amountMediumScore = 0.20;
    private double amountLowScore = 0.05;

    // Location scoring
    private double locationHighScore = 0.25;
    private double locationLowScore = 0.05;

    // Merchant scoring
    private double merchantHighScore = 0.20;
    private double merchantLowScore = 0.05;

    // Frequency scoring
    private int frequencyHighThreshold = 8;
    private int frequencyMedThreshold = 5;
    private double frequencyHighScore = 0.15;
    private double frequencyMedScore = 0.10;

    // Fraud history scoring
    private double fraudCountMultiplier = 0.05;
    private double fraudCountMaxBonus = 0.20;

    // Decision thresholds (shared via common config but stored here for
    // risk-engine)
    private double blockThreshold = 0.80;
    private double reviewThreshold = 0.60;
}
