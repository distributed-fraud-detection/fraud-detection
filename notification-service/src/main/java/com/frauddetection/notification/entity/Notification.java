package com.frauddetection.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_id",       columnList = "userId"),
        @Index(name = "idx_notif_transaction_id", columnList = "transactionId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;   // EMAIL | SMS | WEBHOOK

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // SENT | FAILED | PENDING

    @CreationTimestamp
    private LocalDateTime sentAt;

    public enum NotificationType  { EMAIL, SMS, WEBHOOK }
    public enum NotificationStatus { PENDING, SENT, FAILED }
}
