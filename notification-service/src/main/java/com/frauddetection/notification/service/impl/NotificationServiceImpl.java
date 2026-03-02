package com.frauddetection.notification.service.impl;

import com.frauddetection.common.events.FraudDecisionMadeEvent;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.repository.NotificationRepository;
import com.frauddetection.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void processDecision(FraudDecisionMadeEvent event) {
        String message = buildMessage(event.getDecision(), event.getTransactionId());

        Notification notification = Notification.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .type(Notification.NotificationType.EMAIL)
                .message(message)
                .status(Notification.NotificationStatus.SENT)
                .build();

        notificationRepository.save(notification);
        log.info("Notification persisted: userId={}, decision={}, txnId={}",
                event.getUserId(), event.getDecision(), event.getTransactionId());
    }

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
