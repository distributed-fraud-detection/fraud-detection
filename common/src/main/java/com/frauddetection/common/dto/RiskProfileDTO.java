package com.frauddetection.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class RiskProfileDTO {

    private String userId;
    private Double riskScore;
    private String riskLevel; // LOW | MEDIUM | HIGH
    private Integer recentFraudCount;
    private Integer txnFrequency; // transactions in last 60 seconds
    private String topRiskFactor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;

    public RiskProfileDTO() {
    }

    public RiskProfileDTO(String userId, Double riskScore, String riskLevel, Integer recentFraudCount,
            Integer txnFrequency, String topRiskFactor, LocalDateTime lastUpdated) {
        this.userId = userId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.recentFraudCount = recentFraudCount;
        this.txnFrequency = txnFrequency;
        this.topRiskFactor = topRiskFactor;
        this.lastUpdated = lastUpdated;
    }

    private RiskProfileDTO(Builder builder) {
        this.userId = builder.userId;
        this.riskScore = builder.riskScore;
        this.riskLevel = builder.riskLevel;
        this.recentFraudCount = builder.recentFraudCount;
        this.txnFrequency = builder.txnFrequency;
        this.topRiskFactor = builder.topRiskFactor;
        this.lastUpdated = builder.lastUpdated;
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

    public Integer getRecentFraudCount() {
        return recentFraudCount;
    }

    public void setRecentFraudCount(Integer recentFraudCount) {
        this.recentFraudCount = recentFraudCount;
    }

    public Integer getTxnFrequency() {
        return txnFrequency;
    }

    public void setTxnFrequency(Integer txnFrequency) {
        this.txnFrequency = txnFrequency;
    }

    public String getTopRiskFactor() {
        return topRiskFactor;
    }

    public void setTopRiskFactor(String topRiskFactor) {
        this.topRiskFactor = topRiskFactor;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private Double riskScore;
        private String riskLevel;
        private Integer recentFraudCount;
        private Integer txnFrequency;
        private String topRiskFactor;
        private LocalDateTime lastUpdated;

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

        public Builder recentFraudCount(Integer recentFraudCount) {
            this.recentFraudCount = recentFraudCount;
            return this;
        }

        public Builder txnFrequency(Integer txnFrequency) {
            this.txnFrequency = txnFrequency;
            return this;
        }

        public Builder topRiskFactor(String topRiskFactor) {
            this.topRiskFactor = topRiskFactor;
            return this;
        }

        public Builder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public RiskProfileDTO build() {
            return new RiskProfileDTO(this);
        }
    }
}
