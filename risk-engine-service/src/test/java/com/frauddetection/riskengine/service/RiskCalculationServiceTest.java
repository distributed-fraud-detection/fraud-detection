package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.config.RiskThresholds;
import com.frauddetection.riskengine.service.scoring.AmountScoringFactor;
import com.frauddetection.riskengine.service.scoring.FrequencyScoringFactor;
import com.frauddetection.riskengine.service.scoring.FraudHistoryScoringFactor;
import com.frauddetection.riskengine.service.scoring.LocationScoringFactor;
import com.frauddetection.riskengine.service.scoring.MerchantScoringFactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCalculationServiceTest {

    private RiskCalculationService service;

    @BeforeEach
    void setUp() {
        // Use default thresholds (all fields pre-initialized via Lombok @Data defaults)
        RiskThresholds thresholds = new RiskThresholds();
        service = new RiskCalculationService(List.of(
                new AmountScoringFactor(thresholds),
                new LocationScoringFactor(thresholds),
                new MerchantScoringFactor(thresholds),
                new FrequencyScoringFactor(thresholds),
                new FraudHistoryScoringFactor(thresholds)));
    }

    private TransactionCreatedEvent event(double amount, String location, String merchantType) {
        return TransactionCreatedEvent.builder()
                .transactionId("tx-test-001")
                .userId("u001")
                .amount(BigDecimal.valueOf(amount))
                .location(location)
                .merchantType(merchantType)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private RiskContext ctx(int fraudCount, int txnFrequency) {
        return new RiskContext(fraudCount, txnFrequency);
    }

    @Test
    @DisplayName("Low-amount domestic transaction should score LOW risk")
    void lowAmountDomestic_shouldBeLow() {
        double score = service.calculateRiskScore(event(500, "Mumbai", "E-Commerce"), ctx(0, 2));
        assertThat(score).isLessThan(0.5);
    }

    @Test
    @DisplayName("Amount > 10000 should add high amount weight")
    void highAmount_addsHighWeight() {
        double high = service.calculateRiskScore(event(15000, "Bangalore", "POS Purchase"), ctx(0, 1));
        double low = service.calculateRiskScore(event(500, "Bangalore", "POS Purchase"), ctx(0, 1));
        assertThat(high).isGreaterThan(low);
    }

    @Test
    @DisplayName("Offshore/flagged locations should increase score")
    void offshoreLocation_increasesScore() {
        double domestic = service.calculateRiskScore(event(1000, "Mumbai", "E-Commerce"), ctx(0, 1));
        double offshore = service.calculateRiskScore(event(1000, "Lagos", "E-Commerce"), ctx(0, 1));
        assertThat(offshore).isGreaterThan(domestic);
    }

    @Test
    @DisplayName("Crypto/Casino merchant type should increase score")
    void highRiskMerchant_increasesScore() {
        double normal = service.calculateRiskScore(event(1000, "Delhi", "E-Commerce"), ctx(0, 1));
        double risky = service.calculateRiskScore(event(1000, "Delhi", "Crypto Exchange"), ctx(0, 1));
        assertThat(risky).isGreaterThan(normal);
    }

    @Test
    @DisplayName("High frequency (>8 txns/min) should add frequency weight")
    void highFrequency_increasesScore() {
        double low = service.calculateRiskScore(event(500, "Mumbai", "E-Commerce"), ctx(0, 2));
        double high = service.calculateRiskScore(event(500, "Mumbai", "E-Commerce"), ctx(0, 10));
        assertThat(high).isGreaterThan(low);
    }

    @Test
    @DisplayName("Prior fraud history should increase score")
    void fraudHistory_increasesScore() {
        double noHistory = service.calculateRiskScore(event(500, "Mumbai", "E-Commerce"), ctx(0, 1));
        double withHistory = service.calculateRiskScore(event(500, "Mumbai", "E-Commerce"), ctx(3, 1));
        assertThat(withHistory).isGreaterThan(noHistory);
    }

    @Test
    @DisplayName("Score must never exceed 1.0")
    void score_shouldBeClamped() {
        double score = service.calculateRiskScore(event(99999, "Lagos", "Crypto Exchange"), ctx(10, 20));
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("Score must never be negative")
    void score_shouldBePositive() {
        double score = service.calculateRiskScore(event(100, "Bangalore", "E-Commerce"), ctx(0, 0));
        assertThat(score).isGreaterThanOrEqualTo(0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.85, HIGH",
            "0.65, MEDIUM",
            "0.30, LOW"
    })
    @DisplayName("Risk level derivation thresholds")
    void riskLevelThresholds(double score, String expectedLevel) {
        String level = service.deriveRiskLevel(score);
        assertThat(level).isEqualTo(expectedLevel);
    }
}
