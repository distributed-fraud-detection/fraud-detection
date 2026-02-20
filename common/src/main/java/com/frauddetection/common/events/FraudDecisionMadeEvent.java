package com.frauddetection.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class FraudDecisionMadeEvent {

    private String eventId;
    private String caseId;
    private String transactionId;
    private String userId;
    private String decision; // APPROVE | BLOCK | REVIEW
    private Double riskScore;
    private String flagReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime decidedAt;

    public FraudDecisionMadeEvent() {
    }

    public FraudDecisionMadeEvent(String eventId, String caseId, String transactionId, String userId, String decision,
            Double riskScore, String flagReason, LocalDateTime decidedAt) {
        this.eventId = eventId;
        this.caseId = caseId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.flagReason = flagReason;
        this.decidedAt = decidedAt;
    }

    private FraudDecisionMadeEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.caseId = builder.caseId;
        this.transactionId = builder.transactionId;
        this.userId = builder.userId;
        this.decision = builder.decision;
        this.riskScore = builder.riskScore;
        this.flagReason = builder.flagReason;
        this.decidedAt = builder.decidedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
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

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String caseId;
        private String transactionId;
        private String userId;
        private String decision;
        private Double riskScore;
        private String flagReason;
        private LocalDateTime decidedAt;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder caseId(String caseId) {
            this.caseId = caseId;
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

        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public Builder riskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder flagReason(String flagReason) {
            this.flagReason = flagReason;
            return this;
        }

        public Builder decidedAt(LocalDateTime decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public FraudDecisionMadeEvent build() {
            return new FraudDecisionMadeEvent(this);
        }
    }
}
