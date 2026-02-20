package com.frauddetection.riskengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feature_weights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String featureName;     // e.g. HIGH_AMOUNT, UNKNOWN_LOCATION, HIGH_FREQUENCY

    @Column(nullable = false)
    private Double weight;          // Contribution to risk score (0.0 - 1.0)

    @Column
    private String description;
}
