package com.frauddetection.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In-memory projection of a fraud_cases row read by the Spring Batch ItemReader.
 * Uses JDBC RowMapper, not JPA — reads from fraud_db via a separate DataSource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCaseRow {
    private String transactionId;
    private String userId;
    private Double riskScore;
    private String decision;   // APPROVE | BLOCK | REVIEW
    private String location;   // will be populated from joined transaction data if available
}
