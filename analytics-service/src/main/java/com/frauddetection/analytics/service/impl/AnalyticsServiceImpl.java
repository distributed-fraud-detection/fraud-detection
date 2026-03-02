package com.frauddetection.analytics.service.impl;

import com.frauddetection.analytics.batch.BatchJobLauncher;
import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.repository.AggregatedMetricRepository;
import com.frauddetection.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AggregatedMetricRepository metricRepository;
    private final BatchJobLauncher batchJobLauncher;

    @Override
    public List<AggregatedMetric> getDailySummary(int days) {
        return metricRepository.findRecentMetrics(PageRequest.of(0, days));
    }

    @Override
    public List<AggregatedMetric> getTopRiskUsers(int limit) {
        return metricRepository.findRecentMetrics(PageRequest.of(0, limit));
    }

    @Override
    public void triggerDailyBatch() throws Exception {
        log.info("Manual batch trigger requested");
        batchJobLauncher.launchDailyJob();
        log.info("DailyFraudAnalyticsJob launched successfully");
    }
}
