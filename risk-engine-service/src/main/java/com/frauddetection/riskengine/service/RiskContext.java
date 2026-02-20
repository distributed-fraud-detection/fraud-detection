package com.frauddetection.riskengine.service;

import lombok.Value;

/**
 * Immutable value object carrying contextual risk data from Redis.
 *
 * SOLID Fix: SRP + Value Object Pattern
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: recentFraudCount and txnFrequency were passed as separate int
 * parameters,
 * making the calculateRiskScore() signature grow with each new context field.
 *
 * AFTER: A single RiskContext carries all context. Adding a new field (e.g.,
 * vpnDetected)
 * only changes the Value Object construction, not every method signature.
 *
 * Lombok @Value makes this immutable (all fields final, no setters).
 */
@Value
public class RiskContext {

    /**
     * Number of historical BLOCK/REVIEW decisions for this user in the last 24h.
     */
    int recentFraudCount;

    /**
     * Transaction count for this user in the last 60 seconds (rate-limiting
     * window).
     */
    int txnFrequency;
}
