package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.entity.RiskProfile;
import com.frauddetection.riskengine.repository.RiskProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock
    private RiskCalculationService riskCalculationService;
    @Mock
    private RedisCacheService redisCacheService;
    @Mock
    private RiskProfileRepository riskProfileRepository;
    @Mock
    private KafkaTemplate<String, RiskScoredEvent> kafkaTemplate;

    @InjectMocks
    private RiskEngineService riskEngineService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskEngineService, "riskScoredTopic", "risk.scored");
    }

    private TransactionCreatedEvent event(String transactionId, String userId) {
        return TransactionCreatedEvent.builder()
                .transactionId(transactionId)
                .userId(userId)
                .amount(BigDecimal.valueOf(350))
                .merchantType("E-Commerce")
                .location("NY")
                .build();
    }

    @Test
    @DisplayName("evaluate: high risk updates cache/profile and hot-lists transaction")
    void evaluate_highRisk_updatesAllAndHotLists() {
        TransactionCreatedEvent event = event("txn-high", "user-1");

        when(redisCacheService.getRecentFraudCount("user-1")).thenReturn(3);
        when(redisCacheService.getRecentTxnCount("user-1")).thenReturn(10);
        when(riskCalculationService.calculateRiskScore(eq(event), any(RiskContext.class))).thenReturn(0.87);
        when(riskCalculationService.deriveRiskLevel(0.87)).thenReturn("HIGH");
        when(riskProfileRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        riskEngineService.evaluate(event);

        verify(redisCacheService).cacheRiskScore("user-1", 0.87, "HIGH");
        verify(redisCacheService).addToHotList("txn-high");

        ArgumentCaptor<RiskProfile> profileCaptor = ArgumentCaptor.forClass(RiskProfile.class);
        verify(riskProfileRepository).save(profileCaptor.capture());
        RiskProfile saved = profileCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getRiskScore()).isEqualTo(0.87);
        assertThat(saved.getRiskLevel()).isEqualTo("HIGH");
        assertThat(saved.getRecentFraudCount()).isEqualTo(3);
        assertThat(saved.getTxnFrequency()).isEqualTo(10);

        verify(kafkaTemplate).send(eq("risk.scored"), eq("user-1"), any(RiskScoredEvent.class));
    }

    @Test
    @DisplayName("evaluate: existing profile is updated and non-high risk is not hot-listed")
    void evaluate_existingProfile_nonHighRisk_skipsHotList() {
        TransactionCreatedEvent event = event("txn-med", "user-2");
        RiskProfile existing = RiskProfile.builder().id(99L).userId("user-2").build();

        when(redisCacheService.getRecentFraudCount("user-2")).thenReturn(1);
        when(redisCacheService.getRecentTxnCount("user-2")).thenReturn(4);
        when(riskCalculationService.calculateRiskScore(eq(event), any(RiskContext.class))).thenReturn(0.44);
        when(riskCalculationService.deriveRiskLevel(0.44)).thenReturn("MEDIUM");
        when(riskProfileRepository.findByUserId("user-2")).thenReturn(Optional.of(existing));

        riskEngineService.evaluate(event);

        verify(redisCacheService).cacheRiskScore("user-2", 0.44, "MEDIUM");
        verify(redisCacheService, never()).addToHotList(anyString());

        ArgumentCaptor<RiskProfile> profileCaptor = ArgumentCaptor.forClass(RiskProfile.class);
        verify(riskProfileRepository).save(profileCaptor.capture());
        RiskProfile saved = profileCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getUserId()).isEqualTo("user-2");
        assertThat(saved.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(saved.getRiskScore()).isEqualTo(0.44);
    }
}
