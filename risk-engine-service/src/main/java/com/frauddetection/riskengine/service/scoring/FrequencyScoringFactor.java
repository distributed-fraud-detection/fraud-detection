package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Scores based on velocity — how many transactions the user sent in the last 60
 * seconds.
 * High velocity is a classic card-testing / account-takeover signal.
 */
@Component
@RequiredArgsConstructor
public class FrequencyScoringFactor implements RiskScoringFactor {

    private final RiskThresholds thresholds;

    @Override
    public double score(TransactionCreatedEvent event, RiskContext context) {
        int frequency = context.getTxnFrequency();

        if (frequency > thresholds.getFrequencyHighThreshold()) {
            return thresholds.getFrequencyHighScore();
        }
        if (frequency > thresholds.getFrequencyMedThreshold()) {
            return thresholds.getFrequencyMedScore();
        }
        return 0.0; // Low frequency — no additional risk
    }

    @Override
    public String name() {
        return "FrequencyFactor";
    }
}
