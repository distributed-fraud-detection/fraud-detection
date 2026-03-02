package com.frauddetection.frauddecision.service;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import com.frauddetection.frauddecision.rule.DecisionResult;
import com.frauddetection.frauddecision.rule.DecisionRule;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDecisionServiceTest {

    @Mock
    private DecisionRule rule1;
    @Mock
    private DecisionRule rule2;
    @Mock
    private FraudCaseRepository fraudCaseRepository;
    @Mock
    private KafkaTemplate<String, FraudDecisionMadeEvent> kafkaTemplate;

    @InjectMocks
    private FraudDecisionService fraudDecisionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fraudDecisionService, "fraudDecisionTopic", "fraud.decision.made");
        ReflectionTestUtils.setField(fraudDecisionService, "decisionRules", List.of(rule1, rule2));
    }

    @Test
    @DisplayName("process: first matching rule persists case and publishes decision event")
    void process_matchingRule_persistsAndPublishes() {
        RiskScoredEvent event = RiskScoredEvent.builder()
                .transactionId("txn-1")
                .userId("user-1")
                .riskScore(0.91)
                .riskLevel("HIGH")
                .build();

        DecisionResult result = DecisionResult.builder()
                .decision(FraudCase.Decision.BLOCK)
                .status(FraudCase.CaseStatus.BLOCKED)
                .flagReason("High fraud score")
                .build();

        when(rule1.matches(event)).thenReturn(false);
        when(rule2.matches(event)).thenReturn(true);
        when(rule2.apply(event)).thenReturn(result);

        fraudDecisionService.process(event);

        ArgumentCaptor<FraudCase> caseCaptor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(caseCaptor.capture());
        FraudCase saved = caseCaptor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo("txn-1");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getRiskScore()).isEqualTo(0.91);
        assertThat(saved.getDecision()).isEqualTo(FraudCase.Decision.BLOCK);
        assertThat(saved.getStatus()).isEqualTo(FraudCase.CaseStatus.BLOCKED);
        assertThat(saved.getFlagReason()).isEqualTo("High fraud score");

        verify(kafkaTemplate).send(eq("fraud.decision.made"), eq("user-1"), any(FraudDecisionMadeEvent.class));
    }

    @Test
    @DisplayName("process: throws when no decision rule matches")
    void process_noMatchingRule_throws() {
        RiskScoredEvent event = RiskScoredEvent.builder()
                .transactionId("txn-no-rule")
                .userId("user-2")
                .riskScore(0.2)
                .build();

        when(rule1.matches(event)).thenReturn(false);
        when(rule2.matches(event)).thenReturn(false);

        assertThatThrownBy(() -> fraudDecisionService.process(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No decision rule matched");

        verify(fraudCaseRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
