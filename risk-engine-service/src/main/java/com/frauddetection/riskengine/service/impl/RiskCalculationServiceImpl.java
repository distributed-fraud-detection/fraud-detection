package com.frauddetection.riskengine.service.impl;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.service.RiskCalculationService;
import com.frauddetection.riskengine.service.RiskContext;
import com.frauddetection.riskengine.service.scoring.RiskScoringFactor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationServiceImpl implements RiskCalculationService {

    private final List<RiskScoringFactor> factors;

    @Override
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

    @Override
    public String deriveRiskLevel(double score) {
        if (score >= 0.80)
            return "HIGH";
        if (score >= 0.60)
            return "MEDIUM";
        return "LOW";
    }
}
