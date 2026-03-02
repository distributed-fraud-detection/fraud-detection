package com.frauddetection.frauddecision.service;

import com.frauddetection.common.events.RiskScoredEvent;

public interface FraudDecisionService {

    void process(RiskScoredEvent event);
}
