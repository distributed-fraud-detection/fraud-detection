package com.frauddetection.analytics.service;

import com.frauddetection.analytics.batch.BatchJobLauncher;
import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.repository.AggregatedMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Analytics service — the business logic layer between controller and
 * repository.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: AnalyticsController directly injected AggregatedMetricRepository
 * and BatchJobLauncher, mixing HTTP-layer and service-layer concerns.
 *
 * AFTER: Controller → AnalyticsService → Repository + BatchJobLauncher.
 * The controller is now a pure HTTP adapter (no business logic).
 * AnalyticsService is independently testable without Spring MVC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AggregatedMetricRepository metricRepository;
    private final BatchJobLauncher batchJobLauncher;

    /**
     * Returns the most recent N days of aggregated fraud metrics.
     */
    public List<AggregatedMetric> getDailySummary(int days) {
        return metricRepository.findRecentMetrics(PageRequest.of(0, days));
    }

    /**
     * Returns the top N metrics records (by most recent — in production this
     * would sort by risk score or join with risk_db).
     */
    public List<AggregatedMetric> getTopRiskUsers(int limit) {
        return metricRepository.findRecentMetrics(PageRequest.of(0, limit));
    }

    /**
     * Triggers the batch job manually.
     *
     * @throws Exception if job launch fails (propagated to controller for error
     *                   handling)
     */
    public void triggerDailyBatch() throws Exception {
        log.info("Manual batch trigger requested");
        batchJobLauncher.launchDailyJob();
        log.info("DailyFraudAnalyticsJob launched successfully");
    }
}
