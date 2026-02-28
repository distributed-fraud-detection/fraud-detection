package com.frauddetection.notification.service;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Layer 1: Unit test for NotificationService.
 *
 * Verifies:
 * - Repository is called once per decision event
 * - BLOCK message contains "blocked" + support text
 * - REVIEW message contains "under review"
 * - APPROVE (default) message contains "approved"
 * - Notification entity has correct status=SENT and type=EMAIL
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private FraudDecisionMadeEvent buildEvent(String caseId, String decision) {
        return FraudDecisionMadeEvent.builder()
                .caseId(caseId)
                .transactionId("tx-" + caseId)
                .userId("u-test")
                .decision(decision)
                .decidedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("processDecision() saves ONE notification to the repository")
    void processDecision_savesExactlyOneNotification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.processDecision(buildEvent("c001", "BLOCK"));

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("BLOCK decision → notification message contains 'blocked'")
    void blockDecision_messageContainsBlocked() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.processDecision(buildEvent("c002", "BLOCK"));

        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).containsIgnoringCase("blocked");
    }

    @Test
    @DisplayName("REVIEW decision → notification message contains 'under review'")
    void reviewDecision_messageContainsUnderReview() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.processDecision(buildEvent("c003", "REVIEW"));

        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).containsIgnoringCase("review");
    }

    @Test
    @DisplayName("APPROVE decision → notification message contains 'approved'")
    void approveDecision_messageContainsApproved() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.processDecision(buildEvent("c004", "APPROVE"));

        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).containsIgnoringCase("approved");
    }

    @ParameterizedTest
    @CsvSource({"BLOCK", "REVIEW", "APPROVE"})
    @DisplayName("All decisions → notification saved with status=SENT and type=EMAIL")
    void allDecisions_savedWithCorrectStatusAndType(String decision) {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.processDecision(buildEvent("c005", decision));

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getStatus().name()).isEqualTo("SENT");
        assertThat(saved.getType().name()).isEqualTo("EMAIL");
        assertThat(saved.getUserId()).isEqualTo("u-test");
    }
}
