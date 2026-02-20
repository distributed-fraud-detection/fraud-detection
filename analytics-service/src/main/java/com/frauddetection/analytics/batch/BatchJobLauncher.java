package com.frauddetection.analytics.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job dailyFraudAnalyticsJob;

    /**
     * Runs automatically every day at 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledDailyJob() {
        log.info("Scheduled trigger: starting DailyFraudAnalyticsJob");
        try {
            launchDailyJob();
        } catch (Exception e) {
            log.error("Scheduled daily analytics job failed", e);
        }
    }

    /**
     * Can also be called manually via the REST endpoint.
     */
    public void launchDailyJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(dailyFraudAnalyticsJob, params);
        log.info("DailyFraudAnalyticsJob completed");
    }
}
