package com.frauddetection.riskengine.service.scoring;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Scores based on the transaction's geographical origin.
 * Unknown, offshore, or foreign locations carry higher fraud risk.
 */
@Component
@RequiredArgsConstructor
public class LocationScoringFactor implements RiskScoringFactor {

    private final RiskThresholds thresholds;

    private static final Set<String> HIGH_RISK_LOCATION_KEYWORDS = Set.of("UNKNOWN", "OFFSHORE", "FOREIGN",
            "ANONYMOUS");

    @Override
    public double score(TransactionCreatedEvent event, RiskContext context) {
        String location = event.getLocation();
        if (location == null)
            return thresholds.getLocationHighScore(); // unknown = high risk

        String upper = location.toUpperCase();
        boolean isHighRisk = HIGH_RISK_LOCATION_KEYWORDS.stream()
                .anyMatch(upper::contains);

        return isHighRisk
                ? thresholds.getLocationHighScore()
                : thresholds.getLocationLowScore();
    }

    @Override
    public String name() {
        return "LocationFactor";
    }
}
