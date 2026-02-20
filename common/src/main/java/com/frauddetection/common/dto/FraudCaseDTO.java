package com.frauddetection.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class FraudCaseDTO {

    private String caseId;
    private String transactionId;
    private String userId;
    private Double riskScore;
    private String decision; // APPROVE | BLOCK | REVIEW
    private String status; // PENDING | APPROVED | REJECTED | BLOCKED
    private String flagReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public FraudCaseDTO() {
    }

    public FraudCaseDTO(String caseId, String transactionId, String userId, Double riskScore, String decision,
            String status, String flagReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.caseId = caseId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.status = status;
        this.flagReason = flagReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private FraudCaseDTO(Builder builder) {
        this.caseId = builder.caseId;
        this.transactionId = builder.transactionId;
        this.userId = builder.userId;
        this.riskScore = builder.riskScore;
        this.decision = builder.decision;
        this.status = builder.status;
        this.flagReason = builder.flagReason;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
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

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String caseId;
        private String transactionId;
        private String userId;
        private Double riskScore;
        private String decision;
        private String status;
        private String flagReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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

        public Builder riskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder flagReason(String flagReason) {
            this.flagReason = flagReason;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public FraudCaseDTO build() {
            return new FraudCaseDTO(this);
        }
    }
}
