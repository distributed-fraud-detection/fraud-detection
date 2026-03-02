package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.TransactionCreatedEvent;

public interface RiskEngineService {

    void evaluate(TransactionCreatedEvent event);
}
