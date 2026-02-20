package com.frauddetection.transaction.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity — Transaction.
 *
 * SOLID Fix: Replaced Lombok @Data with explicit annotations.
 * ─────────────────────────────────────────────────────────
 * WHY @Data IS WRONG ON JPA ENTITIES:
 * 1. @Data generates setters → entity is mutable → Hibernate can call them
 * inadvertently during lazy-loading proxy creation, causing bugs.
 * 2. @Data generates hashCode/equals based on ALL fields including the
 * surrogate PK (id), which is null before persist → Set/HashMap breaks.
 * 3. @Data generates toString including all fields → can accidentally
 * trigger lazy collection loading (LazyInitializationException).
 *
 * CORRECT APPROACH:
 * - @Getter → read-only by default (immutability-first)
 * - @EqualsAndHashCode(of = "transactionId") → stable, business-key based
 * - @ToString(exclude = {"id"}) → safe, no lazy-load triggers
 * - @NoArgsConstructor(access = PROTECTED) → Hibernate needs it but stops
 * direct instantiation; use Builder pattern instead.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // Hibernate only
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE) // Builder only
@EqualsAndHashCode(of = "transactionId") // Business key — stable across persist lifecycle
@ToString(exclude = "id") // Exclude surrogate PK from toString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String transactionId;

    @Column(nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private String location;

    @Column(nullable = false, updatable = false)
    private String merchantType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public enum TransactionStatus {
        PENDING, APPROVED, BLOCKED, FLAGGED
    }

    /**
     * Explicitly set a new status — the only intentional mutation allowed.
     * All other fields are immutable after construction.
     */
    public Transaction withStatus(TransactionStatus newStatus) {
        return Transaction.builder()
                .id(this.id)
                .transactionId(this.transactionId)
                .userId(this.userId)
                .amount(this.amount)
                .location(this.location)
                .merchantType(this.merchantType)
                .status(newStatus)
                .timestamp(this.timestamp)
                .build();
    }

    @PrePersist
    protected void prePersist() {
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }
}
