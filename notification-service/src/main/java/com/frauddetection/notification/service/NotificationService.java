package com.frauddetection.notification.service;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for notification business logic.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: NotificationConsumer had notification construction, message building,
 * and repository persistence all inside the @KafkaListener method — the
 * Kafka adapter was doing service-layer work.
 *
 * AFTER: NotificationConsumer is now a thin Kafka adapter that delegates to
 * this service. This class has ONE job: build and persist notifications.
 *
 * Benefits:
 * - NotificationService can be unit-tested without any Kafka machinery
 * - The @KafkaListener can change independently of business logic
 * - Easier to add async email/SMS dispatch without touching the consumer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Processes a fraud decision event and persists a notification record.
     *
     * @param event the FraudDecisionMadeEvent received from Kafka
     */
    public void processDecision(FraudDecisionMadeEvent event) {
        String message = buildMessage(event.getDecision(), event.getTransactionId());

        Notification notification = Notification.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .type(Notification.NotificationType.EMAIL) // extend for SMS/WEBHOOK later
                .message(message)
                .status(Notification.NotificationStatus.SENT) // simulated send
                .build();

        notificationRepository.save(notification);
        log.info("Notification persisted: userId={}, decision={}, txnId={}",
                event.getUserId(), event.getDecision(), event.getTransactionId());
    }

    /**
     * Builds the user-facing notification message based on fraud decision.
     */
    private String buildMessage(String decision, String transactionId) {
        return switch (decision.toUpperCase()) {
            case "BLOCK" -> "Your transaction " + transactionId
                    + " has been blocked due to suspicious activity. "
                    + "Please contact support if this was legitimate.";
            case "REVIEW" -> "Your transaction " + transactionId
                    + " is under review by our fraud prevention team. "
                    + "We will notify you once the review is complete.";
            default -> "Your transaction " + transactionId
                    + " has been approved successfully.";
        };
    }
}
