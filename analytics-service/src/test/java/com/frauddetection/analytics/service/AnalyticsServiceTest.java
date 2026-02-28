package com.frauddetection.analytics.service;

import com.frauddetection.analytics.batch.BatchJobLauncher;
import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.repository.AggregatedMetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Layer 1: Unit test for AnalyticsService.
 *
 * Verifies:
 * - getDailySummary() delegates to repository with correct PageRequest
 * - getTopRiskUsers() uses the limit parameter correctly
 * - triggerDailyBatch() delegates to BatchJobLauncher
 * - triggerDailyBatch() propagates exceptions (tested by controller for 500
 * response)
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AggregatedMetricRepository metricRepository;

    @Mock
    private BatchJobLauncher batchJobLauncher;

    @InjectMocks
    private AnalyticsService analyticsService;

    private AggregatedMetric metric(double fraudRate) {
        AggregatedMetric m = new AggregatedMetric();
        m.setMetricDate(LocalDate.now().minusDays(1));
        m.setTotalTransactions(200L);
        m.setFraudCount((long) (200 * fraudRate));
        m.setFraudRate(fraudRate);
        return m;
    }

    @Test
    @DisplayName("getDailySummary(14) → delegates to repo with PageRequest(0, 14)")
    void getDailySummary_delegatesToRepo() {
        when(metricRepository.findRecentMetrics(PageRequest.of(0, 14)))
                .thenReturn(List.of(metric(0.10)));

        List<AggregatedMetric> result = analyticsService.getDailySummary(14);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFraudRate()).isEqualTo(0.10);
        verify(metricRepository).findRecentMetrics(PageRequest.of(0, 14));
    }

    @Test
    @DisplayName("getTopRiskUsers(5) → delegates to repo with PageRequest(0, 5)")
    void getTopRiskUsers_usesExactLimit() {
        when(metricRepository.findRecentMetrics(PageRequest.of(0, 5)))
                .thenReturn(List.of(metric(0.80), metric(0.75)));

        List<AggregatedMetric> result = analyticsService.getTopRiskUsers(5);

        assertThat(result).hasSize(2);
        verify(metricRepository).findRecentMetrics(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("triggerDailyBatch() → calls batchJobLauncher.launchDailyJob()")
    void triggerDailyBatch_callsLauncher() throws Exception {
        doNothing().when(batchJobLauncher).launchDailyJob();

        analyticsService.triggerDailyBatch();

        verify(batchJobLauncher, times(1)).launchDailyJob();
    }

    @Test
    @DisplayName("triggerDailyBatch() → propagates exception from launcher")
    void triggerDailyBatch_propagatesException() throws Exception {
        doThrow(new RuntimeException("Kafka not available")).when(batchJobLauncher).launchDailyJob();

        assertThatThrownBy(() -> analyticsService.triggerDailyBatch())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kafka not available");
    }
}
