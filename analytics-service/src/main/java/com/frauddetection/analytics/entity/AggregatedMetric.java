package com.frauddetection.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "aggregated_metrics", indexes = {
        @Index(name = "idx_metric_date", columnList = "metricDate", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate metricDate;

    private Long totalTransactions;
    private Long fraudCount;
    private Long reviewCount;
    private Long blockCount;
    private Double fraudRate;        // fraudCount / totalTransactions
    private String topRiskGeography;
    private Double avgRiskScore;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
