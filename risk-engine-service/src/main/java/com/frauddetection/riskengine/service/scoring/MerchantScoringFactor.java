package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Scores the transaction based on the merchant's known risk category.
 * Casino, crypto, and gambling merchants have significantly elevated fraud
 * rates.
 */
@Component
@RequiredArgsConstructor
public class MerchantScoringFactor implements RiskScoringFactor {

    private final RiskThresholds thresholds;

    // High-risk merchant types — configurable if driven from DB in future
    private static final Set<String> HIGH_RISK_MERCHANTS = Set.of("CASINO", "CRYPTO", "GAMBLING", "CRYPTOCURRENCY",
            "DARKNET");

    @Override
    public double score(TransactionCreatedEvent event, RiskContext context) {
        String merchant = event.getMerchantType();
        if (merchant == null)
            return thresholds.getMerchantLowScore();

        boolean isHighRisk = HIGH_RISK_MERCHANTS.stream()
                .anyMatch(k -> merchant.toUpperCase().contains(k));

        return isHighRisk
                ? thresholds.getMerchantHighScore()
                : thresholds.getMerchantLowScore();
    }

    @Override
    public String name() {
        return "MerchantFactor";
    }
}
