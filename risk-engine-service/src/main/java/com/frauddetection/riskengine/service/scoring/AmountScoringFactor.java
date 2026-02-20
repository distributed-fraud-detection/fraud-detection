package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Scores the transaction based on monetary amount.
 * High amounts are statistically more likely to be fraudulent.
 */
@Component
@RequiredArgsConstructor
public class AmountScoringFactor implements RiskScoringFactor {

    private final RiskThresholds thresholds;

    @Override
    public double score(TransactionCreatedEvent event, RiskContext context) {
        BigDecimal amount = event.getAmount();
        if (amount == null)
            return thresholds.getAmountLowScore();

        double highAmt = thresholds.getHighAmountThreshold();
        double medAmt = thresholds.getMediumAmountThreshold();

        if (amount.doubleValue() > highAmt)
            return thresholds.getAmountHighScore();
        if (amount.doubleValue() > medAmt)
            return thresholds.getAmountMediumScore();
        return thresholds.getAmountLowScore();
    }

    @Override
    public String name() {
        return "AmountFactor";
    }
}
