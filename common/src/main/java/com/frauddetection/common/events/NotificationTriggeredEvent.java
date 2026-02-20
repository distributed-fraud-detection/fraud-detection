package com.frauddetection.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class NotificationTriggeredEvent {

    private String eventId;
    private String transactionId;
    private String userId;
    private String notificationType; // EMAIL | SMS | WEBHOOK
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime triggeredAt;

    public NotificationTriggeredEvent() {
    }

    public NotificationTriggeredEvent(String eventId, String transactionId, String userId, String notificationType,
            String message, LocalDateTime triggeredAt) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.message = message;
        this.triggeredAt = triggeredAt;
    }

    private NotificationTriggeredEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.transactionId = builder.transactionId;
        this.userId = builder.userId;
        this.notificationType = builder.notificationType;
        this.message = builder.message;
        this.triggeredAt = builder.triggeredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String transactionId;
        private String userId;
        private String notificationType;
        private String message;
        private LocalDateTime triggeredAt;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder notificationType(String notificationType) {
            this.notificationType = notificationType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder triggeredAt(LocalDateTime triggeredAt) {
            this.triggeredAt = triggeredAt;
            return this;
        }

        public NotificationTriggeredEvent build() {
            return new NotificationTriggeredEvent(this);
        }
    }
}
