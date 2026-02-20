package com.frauddetection.analytics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Multi-datasource configuration for analytics-service:
 * - Primary:   analytics_db (port 5436) — owns AggregatedMetric + Spring Batch metadata tables
 * - Secondary: fraud_db     (port 5434) — read-only for the batch ItemReader
 */
@Configuration
public class DataSourceConfig {

    /**
     * Primary datasource: analytics_db.
     * Annotated @Primary so Spring Boot auto-configuration (JPA, Batch) uses this one.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Secondary datasource: fraud_db (read-only JDBC for Spring Batch reader).
     */
    @Bean(name = "fraudDataSource")
    @ConfigurationProperties(prefix = "fraud.datasource")
    public DataSource fraudDataSource() {
        return DataSourceBuilder.create().build();
    }
}
