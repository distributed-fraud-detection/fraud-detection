package com.frauddetection.analytics;

import com.frauddetection.analytics.entity.AggregatedMetric;
import com.frauddetection.analytics.repository.AggregatedMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Layer 3: Integration test for the DailyFraudAnalytics Spring Batch job.
 *
 * Tests:
 * 1. Seeds fraud_cases table with yesterday's data (mix of
 * BLOCK/REVIEW/APPROVE)
 * 2. Manually runs the batch job via JobLauncher
 * 3. Asserts AggregatedMetric row is created with correct aggregated values
 *
 * Uses Testcontainers: 2 real PostgreSQL instances (analytics_db + fraud_db)
 * mirrored from the docker-compose multi-datasource setup.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AnalyticsBatchJobIT {

    @Container
    static final PostgreSQLContainer<?> ANALYTICS_DB = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("analytics_db")
            .withUsername("fraud_user")
            .withPassword("fraud_pass");

    @Container
    static final PostgreSQLContainer<?> FRAUD_DB = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("fraud_db")
            .withUsername("fraud_user")
            .withPassword("fraud_pass");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Primary datasource → analytics_db (JPA + Batch metadata)
        registry.add("spring.datasource.url", ANALYTICS_DB::getJdbcUrl);
        registry.add("spring.datasource.username", ANALYTICS_DB::getUsername);
        registry.add("spring.datasource.password", ANALYTICS_DB::getPassword);
        // Secondary datasource → fraud_db (batch reader source)
        registry.add("fraud.datasource.url", FRAUD_DB::getJdbcUrl);
        registry.add("fraud.datasource.username", FRAUD_DB::getUsername);
        registry.add("fraud.datasource.password", FRAUD_DB::getPassword);
    }

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job dailyFraudAnalyticsJob;

    @Autowired
    private AggregatedMetricRepository analyticsRepository;

    @Autowired
    @Qualifier("fraudDataSource")
    private DataSource fraudDataSource;

    private JdbcTemplate fraudJdbc;

    @BeforeEach
    void setUp() {
        fraudJdbc = new JdbcTemplate(fraudDataSource);

        // Create the fraud_cases table in the fraud_db container
        fraudJdbc.execute("""
                CREATE TABLE IF NOT EXISTS fraud_cases (
                    case_id       VARCHAR(255) PRIMARY KEY,
                    transaction_id VARCHAR(255),
                    user_id       VARCHAR(255),
                    risk_score    DOUBLE PRECISION,
                    decision      VARCHAR(50),
                    created_at    TIMESTAMP DEFAULT NOW()
                )
                """);

        // Clean previous runs
        fraudJdbc.execute("DELETE FROM fraud_cases");
        analyticsRepository.deleteAll();

        // Seed yesterday's fraud cases: 5 BLOCK, 3 REVIEW, 12 APPROVE (total=20)
        String yesterday = LocalDate.now().minusDays(1).toString();
        insertFraudCase("BLOCK", 0.91, yesterday, 5);
        insertFraudCase("REVIEW", 0.68, yesterday, 3);
        insertFraudCase("APPROVE", 0.30, yesterday, 12);
    }

    private void insertFraudCase(String decision, double score, String date, int count) {
        for (int i = 0; i < count; i++) {
            fraudJdbc.update(
                    "INSERT INTO fraud_cases(case_id, transaction_id, user_id, risk_score, decision, created_at) VALUES (?,?,?,?,?,?::date)",
                    java.util.UUID.randomUUID().toString(),
                    "tx-" + decision.toLowerCase() + "-" + i,
                    "u-batch-user",
                    score,
                    decision,
                    date);
        }
    }

    @Test
    @DisplayName("Batch job processes yesterday's fraud_cases → correct AggregatedMetric saved")
    void batchJob_aggregatesCorrectMetrics() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(dailyFraudAnalyticsJob, params);

        // Batch job must complete successfully
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify aggregated metrics
        List<AggregatedMetric> metrics = analyticsRepository.findAll();
        assertThat(metrics).isNotEmpty();

        AggregatedMetric metric = metrics.get(0);
        assertThat(metric.getTotalTransactions()).isEqualTo(20L); // 5+3+12
        assertThat(metric.getBlockCount()).isEqualTo(5L);
        assertThat(metric.getReviewCount()).isEqualTo(3L);
        assertThat(metric.getApproveCount()).isEqualTo(12L);
        assertThat(metric.getFraudCount()).isEqualTo(8L); // BLOCK+REVIEW
        // fraudRate should be 8/20 = 0.40
        assertThat(metric.getFraudRate()).isCloseTo(0.40, org.assertj.core.data.Offset.offset(0.01));
    }
}
