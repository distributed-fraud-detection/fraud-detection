package com.frauddetection.frauddecision.kafka;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.config.DecisionProperties;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import com.frauddetection.frauddecision.rule.ApproveDecisionRule;
import com.frauddetection.frauddecision.rule.BlockDecisionRule;
import com.frauddetection.frauddecision.rule.ReviewDecisionRule;
import com.frauddetection.frauddecision.service.FraudDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDecisionConsumerTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private KafkaTemplate<String, FraudDecisionMadeEvent> kafkaTemplate;

    private FraudDecisionService service;

    @BeforeEach
    void setUp() {
        when(fraudCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DecisionProperties props = new DecisionProperties(); // uses defaults: block=0.80, review=0.60
        service = new FraudDecisionService(
                List.of(new BlockDecisionRule(props), new ReviewDecisionRule(props), new ApproveDecisionRule()),
                fraudCaseRepository,
                kafkaTemplate
        );
        ReflectionTestUtils.setField(service, "fraudDecisionTopic", "fraud.decision.made");
    }

    private RiskScoredEvent buildEvent(double riskScore) {
        return RiskScoredEvent.builder()
                .transactionId("tx-abc-123")
                .userId("u001")
                .riskScore(riskScore)
                .riskLevel(riskScore > 0.8 ? "HIGH" : riskScore > 0.6 ? "MEDIUM" : "LOW")
                .scoredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Risk score > 0.8 → BLOCK decision")
    void highRiskScore_shouldBlock() {
        service.process(buildEvent(0.85));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision().name()).isEqualTo("BLOCK");
    }

    @Test
    @DisplayName("Risk score between 0.6 and 0.8 → REVIEW decision")
    void mediumRiskScore_shouldReview() {
        service.process(buildEvent(0.70));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision().name()).isEqualTo("REVIEW");
    }

    @Test
    @DisplayName("Risk score < 0.6 → APPROVE decision")
    void lowRiskScore_shouldApprove() {
        service.process(buildEvent(0.35));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision().name()).isEqualTo("APPROVE");
    }

    @Test
    @DisplayName("Decision event is published to Kafka after every decision")
    void shouldPublishDecisionEvent() {
        service.process(buildEvent(0.85));
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(FraudDecisionMadeEvent.class));
    }

    @Test
    @DisplayName("BLOCK decision → FraudCase persisted with correct fields")
    void blockDecision_persistsBlockedStatus() {
        service.process(buildEvent(0.90));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        FraudCase saved = captor.getValue();
        assertThat(saved.getDecision().name()).isEqualTo("BLOCK");
        assertThat(saved.getTransactionId()).isEqualTo("tx-abc-123");
        assertThat(saved.getUserId()).isEqualTo("u001");
    }

    @Test
    @DisplayName("Exact boundary score 0.8 should REVIEW, not BLOCK")
    void exactBoundary_0_8_shouldReview() {
        service.process(buildEvent(0.80));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision().name()).isEqualTo("REVIEW");
    }

    @Test
    @DisplayName("Exact boundary score 0.6 should APPROVE, not REVIEW")
    void exactBoundary_0_6_shouldApprove() {
        service.process(buildEvent(0.60));

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision().name()).isEqualTo("APPROVE");
    }
}
