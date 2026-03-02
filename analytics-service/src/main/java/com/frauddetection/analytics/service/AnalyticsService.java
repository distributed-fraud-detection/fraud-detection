package com.frauddetection.analytics.service;

import com.frauddetection.analytics.entity.AggregatedMetric;

import java.util.List;

public interface AnalyticsService {

    List<AggregatedMetric> getDailySummary(int days);

    List<AggregatedMetric> getTopRiskUsers(int limit);

    void triggerDailyBatch() throws Exception;
}
