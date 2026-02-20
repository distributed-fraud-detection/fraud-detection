package com.frauddetection.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class RiskScoredEvent {

    private String eventId;
    private String transactionId;
    private String userId;
    private Double riskScore;
    private String riskLevel; // LOW | MEDIUM | HIGH

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scoredAt;

    public RiskScoredEvent() {
    }

    public RiskScoredEvent(String eventId, String transactionId, String userId, Double riskScore, String riskLevel,
            LocalDateTime scoredAt) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.scoredAt = scoredAt;
    }

    private RiskScoredEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.transactionId = builder.transactionId;
        this.userId = builder.userId;
        this.riskScore = builder.riskScore;
        this.riskLevel = builder.riskLevel;
        this.scoredAt = builder.scoredAt;
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

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(LocalDateTime scoredAt) {
        this.scoredAt = scoredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String transactionId;
        private String userId;
        private Double riskScore;
        private String riskLevel;
        private LocalDateTime scoredAt;

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

        public Builder riskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder scoredAt(LocalDateTime scoredAt) {
            this.scoredAt = scoredAt;
            return this;
        }

        public RiskScoredEvent build() {
            return new RiskScoredEvent(this);
        }
    }
}
