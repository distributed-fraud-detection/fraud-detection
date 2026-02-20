package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Scores based on the user's recent fraud history (last 24h).
 * Prior fraud decisions are the strongest signal for repeat fraud.
 */
@Component
@RequiredArgsConstructor
public class FraudHistoryScoringFactor implements RiskScoringFactor {

    private final RiskThresholds thresholds;

    @Override
    public double score(TransactionCreatedEvent event, RiskContext context) {
        int fraudCount = context.getRecentFraudCount();
        if (fraudCount <= 0)
            return 0.0;

        // Progressive bonus: each prior fraud adds fraudCountMultiplier, capped at max
        double bonus = fraudCount * thresholds.getFraudCountMultiplier();
        return Math.min(bonus, thresholds.getFraudCountMaxBonus());
    }

    @Override
    public String name() {
        return "FraudHistoryFactor";
    }
}
