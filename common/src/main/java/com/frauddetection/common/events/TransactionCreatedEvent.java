package com.frauddetection.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent {

    private String eventId;
    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String location;
    private String merchantType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public TransactionCreatedEvent() {
    }

    public TransactionCreatedEvent(String eventId, String transactionId, String userId, BigDecimal amount,
            String location, String merchantType, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.location = location;
        this.merchantType = merchantType;
        this.timestamp = timestamp;
    }

    private TransactionCreatedEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.transactionId = builder.transactionId;
        this.userId = builder.userId;
        this.amount = builder.amount;
        this.location = builder.location;
        this.merchantType = builder.merchantType;
        this.timestamp = builder.timestamp;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMerchantType() {
        return merchantType;
    }

    public void setMerchantType(String merchantType) {
        this.merchantType = merchantType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String transactionId;
        private String userId;
        private BigDecimal amount;
        private String location;
        private String merchantType;
        private LocalDateTime timestamp;

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

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder merchantType(String merchantType) {
            this.merchantType = merchantType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public TransactionCreatedEvent build() {
            return new TransactionCreatedEvent(this);
        }
    }
}
