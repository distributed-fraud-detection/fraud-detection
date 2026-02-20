package com.frauddetection.analytics.controller;

import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.service.AnalyticsService;
import com.frauddetection.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Analytics REST controller — pure HTTP adapter.
 *
 * SOLID Fix: SRP + DIP
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: Controller directly injected AggregatedMetricRepository and
 * BatchJobLauncher — it was doing service-layer work.
 *
 * AFTER: Controller only knows about AnalyticsService.
 * All business decisions live in AnalyticsService.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService; // DIP: service, not repo

    /**
     * GET /api/analytics/daily-summary?days=14
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<List<AggregatedMetric>> getDailySummary(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(analyticsService.getDailySummary(days));
    }

    /**
     * GET /api/analytics/top-risk-users?limit=10
     */
    @GetMapping("/top-risk-users")
    public ResponseEntity<List<AggregatedMetric>> getTopRiskUsers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopRiskUsers(limit));
    }

    /**
     * POST /api/analytics/run-batch
     * Returns typed ErrorResponse on failure instead of raw Map.
     */
    @PostMapping("/run-batch")
    public ResponseEntity<?> runBatch(HttpServletRequest req) {
        try {
            analyticsService.triggerDailyBatch();
            return ResponseEntity.ok(
                    Map.of("status", "BATCH_TRIGGERED",
                            "message", "DailyFraudAnalyticsJob launched successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                            e.getMessage(), req.getRequestURI()));
        }
    }
}
