package com.frauddetection.riskengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_profiles", indexes = {
        @Index(name = "idx_risk_user_id", columnList = "userId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private Double riskScore;

    @Column(nullable = false)
    private String riskLevel;   // LOW | MEDIUM | HIGH

    private Integer recentFraudCount;
    private Integer txnFrequency;
    private String topRiskFactor;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
