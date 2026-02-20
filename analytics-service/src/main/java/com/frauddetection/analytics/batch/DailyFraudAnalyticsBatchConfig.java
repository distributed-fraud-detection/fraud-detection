package com.frauddetection.analytics.batch;

import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.model.FraudCaseRow;
import com.frauddetection.analytics.repository.AggregatedMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailyFraudAnalyticsBatchConfig {

        private final JobRepository jobRepository;
        private final PlatformTransactionManager transactionManager;
        private final AggregatedMetricRepository metricRepository;

        @Bean
        public Job dailyFraudAnalyticsJob(Step dailyAggregationStep) {
                return new JobBuilder("DailyFraudAnalyticsJob", jobRepository)
                                .start(dailyAggregationStep)
                                .build();
        }

        @Bean
        public Step dailyAggregationStep(
                        @Qualifier("fraudCaseReader") JdbcCursorItemReader<FraudCaseRow> reader) {
                return new StepBuilder("dailyAggregationStep", jobRepository)
                                .<FraudCaseRow, AggregatedMetric>chunk(100, transactionManager)
                                .reader(reader)
                                .processor(fraudCaseProcessor())
                                .writer(aggregatedMetricWriter())
                                .build();
        }

        /**
         * Reads fraud cases from fraud_db for yesterday.
         *
         * SECURITY FIX: SQL Injection Prevention
         * ─────────────────────────────────────────────────────────────────────────
         * BEFORE: String.formatted() interpolated the date directly into the SQL
         * string.
         * While LocalDate.toString() is safe, this pattern is dangerous —
         * it normalises the habit of string-concatenating values into SQL,
         * which is the #1 cause of SQL injection vulnerabilities.
         *
         * AFTER: Uses preparedStatementSetter() to bind the date as a proper
         * JDBC parameter (?). The driver handles quoting/escaping safely.
         * This is ALWAYS the correct approach, regardless of data type.
         */
        @Bean(name = "fraudCaseReader")
        @StepScope
        public JdbcCursorItemReader<FraudCaseRow> fraudCaseReader(
                        @Qualifier("fraudDataSource") DataSource fraudDataSource) {
                LocalDate yesterday = LocalDate.now().minusDays(1);

                // FIXED: lowercase aliases — PostgreSQL lowercases unquoted identifiers,
                // so camelCase aliases like "transactionId" become "transactionid" in JDBC.
                // Using snake_case aliases ensures consistent column name access.
                String sql = """
                                SELECT transaction_id AS transaction_id,
                                       user_id        AS user_id,
                                       risk_score     AS risk_score,
                                       decision       AS decision
                                FROM fraud_cases
                                WHERE DATE(created_at) = ?
                                """;

                return new JdbcCursorItemReaderBuilder<FraudCaseRow>()
                                .name("fraudCaseReader")
                                .dataSource(fraudDataSource)
                                .sql(sql)
                                .preparedStatementSetter(ps -> ps.setDate(1, java.sql.Date.valueOf(yesterday)))
                                .rowMapper((rs, rowNum) -> {
                                        FraudCaseRow row = new FraudCaseRow();
                                        row.setTransactionId(rs.getString("transaction_id"));
                                        row.setUserId(rs.getString("user_id"));
                                        row.setRiskScore(rs.getDouble("risk_score"));
                                        row.setDecision(rs.getString("decision"));
                                        return row;
                                })
                                .build();
        }

        /**
         * Processes a single FraudCaseRow → AggregatedMetric.
         * In production the aggregation happens across the full chunk via a custom
         * writer.
         * Here we use a simple pass-through and aggregate in the writer.
         */
        @Bean
        public ItemProcessor<FraudCaseRow, AggregatedMetric> fraudCaseProcessor() {
                return item -> item == null ? null
                                : AggregatedMetric.builder()
                                                .metricDate(LocalDate.now().minusDays(1))
                                                .totalTransactions(1L)
                                                .fraudCount("BLOCK".equalsIgnoreCase(item.getDecision()) ? 1L : 0L)
                                                .reviewCount("REVIEW".equalsIgnoreCase(item.getDecision()) ? 1L : 0L)
                                                .blockCount("BLOCK".equalsIgnoreCase(item.getDecision()) ? 1L : 0L)
                                                .avgRiskScore(item.getRiskScore())
                                                .build();
        }

        /**
         * Aggregates all processed rows for a given day into a single AggregatedMetric
         * DB record.
         */
        @Bean
        public ItemWriter<AggregatedMetric> aggregatedMetricWriter() {
                return items -> {
                        if (items.isEmpty())
                                return;
                        LocalDate date = items.getItems().get(0).getMetricDate();

                        long total = items.getItems().size();
                        long fraud = items.getItems().stream()
                                        .mapToLong(m -> m.getFraudCount() != null ? m.getFraudCount() : 0L).sum();
                        long review = items.getItems().stream()
                                        .mapToLong(m -> m.getReviewCount() != null ? m.getReviewCount() : 0L).sum();
                        long block = items.getItems().stream()
                                        .mapToLong(m -> m.getBlockCount() != null ? m.getBlockCount() : 0L).sum();
                        double avgRisk = items.getItems().stream()
                                        .mapToDouble(m -> m.getAvgRiskScore() != null ? m.getAvgRiskScore() : 0.0)
                                        .average().orElse(0.0);

                        AggregatedMetric metric = metricRepository.findByMetricDate(date)
                                        .orElse(AggregatedMetric.builder().metricDate(date).build());

                        metric.setTotalTransactions(total);
                        metric.setFraudCount(fraud);
                        metric.setReviewCount(review);
                        metric.setBlockCount(block);
                        metric.setFraudRate(total > 0 ? (double) fraud / total : 0.0);
                        metric.setAvgRiskScore(avgRisk);

                        metricRepository.save(metric);
                        log.info("Aggregated metrics saved for {}: total={}, fraud={}, review={}", date, total, fraud,
                                        review);
                };
        }
}
