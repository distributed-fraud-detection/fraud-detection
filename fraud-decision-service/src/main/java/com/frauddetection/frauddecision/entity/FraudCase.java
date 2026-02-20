package com.frauddetection.frauddecision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_cases", indexes = {
        @Index(name = "idx_case_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_case_user_id", columnList = "userId"),
        @Index(name = "idx_case_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String caseId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Double riskScore;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Decision decision;  // APPROVE | BLOCK | REVIEW

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CaseStatus status;  // PENDING | APPROVED | REJECTED | BLOCKED

    @Column
    private String flagReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Decision {
        APPROVE, BLOCK, REVIEW
    }

    public enum CaseStatus {
        PENDING, APPROVED, REJECTED, BLOCKED
    }
}
