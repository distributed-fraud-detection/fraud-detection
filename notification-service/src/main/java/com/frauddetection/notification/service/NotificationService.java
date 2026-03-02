package com.frauddetection.notification.service;

import com.frauddetection.common.events.FraudDecisionMadeEvent;

public interface NotificationService {

    void processDecision(FraudDecisionMadeEvent event);
}
