package com.frauddetection.analytics.controller;

import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Layer 2: REST API slice test for AnalyticsController.
 *
 * Covers: daily-summary, top-risk-users, run-batch (success + failure).
 */
@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    private AggregatedMetric sampleMetric() {
        AggregatedMetric m = new AggregatedMetric();
        m.setMetricDate(LocalDate.now().minusDays(1));
        m.setTotalTransactions(100L);
        m.setFraudCount(12L);
        m.setFraudRate(0.12);
        m.setBlockCount(8L);
        m.setReviewCount(4L);
        m.setApproveCount(76L);
        m.setAvgRiskScore(0.61);
        return m;
    }

    @Test
    @DisplayName("GET /api/analytics/daily-summary → 200 with list")
    void getDailySummary_returns200() throws Exception {
        when(analyticsService.getDailySummary(14)).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/analytics/daily-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalTransactions").value(100))
                .andExpect(jsonPath("$[0].fraudCount").value(12));
    }

    @Test
    @DisplayName("GET /api/analytics/top-risk-users → 200 with list")
    void getTopRiskUsers_returns200() throws Exception {
        when(analyticsService.getTopRiskUsers(10)).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/analytics/top-risk-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/analytics/run-batch → 200 BATCH_TRIGGERED")
    void runBatch_success_returns200() throws Exception {
        doNothing().when(analyticsService).triggerDailyBatch();

        mockMvc.perform(post("/api/analytics/run-batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BATCH_TRIGGERED"));
    }

    @Test
    @DisplayName("POST /api/analytics/run-batch → 500 when job launch fails")
    void runBatch_failure_returns500() throws Exception {
        doThrow(new RuntimeException("Batch infra unavailable"))
                .when(analyticsService).triggerDailyBatch();

        mockMvc.perform(post("/api/analytics/run-batch"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Batch infra unavailable"));
    }
}
