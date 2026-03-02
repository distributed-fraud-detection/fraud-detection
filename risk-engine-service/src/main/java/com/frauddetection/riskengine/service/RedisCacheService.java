package com.frauddetection.riskengine.service;

public interface RedisCacheService {

    void cacheRiskScore(String userId, double riskScore, String riskLevel);

    Double getCachedRiskScore(String userId);

    int getRecentFraudCount(String userId);

    void incrementFraudCount(String userId);

    int getRecentTxnCount(String userId);

    void addToHotList(String transactionId);
}
