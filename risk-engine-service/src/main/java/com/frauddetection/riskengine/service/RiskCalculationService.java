package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.TransactionCreatedEvent;

public interface RiskCalculationService {

    double calculateRiskScore(TransactionCreatedEvent event, RiskContext context);

    String deriveRiskLevel(double score);
}
