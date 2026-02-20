package com.frauddetection.riskengine.service;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.entity.RiskProfile;
import com.frauddetection.riskengine.repository.RiskProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the full risk evaluation pipeline for an incoming transaction.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: RiskEngineConsumer was a God class — it directly handled:
 * (1) Redis cache reads (2) Risk score computation (3) Redis cache writes
 * (4) DB upsert (5) Kafka event publishing
 * That's 5 responsibilities in one listener class!
 *
 * AFTER: RiskEngineConsumer is now a thin Kafka adapter.
 * This service handles the complete pipeline in ONE place,
 * and each step delegates to its specialist class:
 * ┌── RedisCacheService → cache reads / writes
 * ├── RiskCalculationService → scoring (via Strategy factors)
 * ├── RiskProfileRepository → DB upsert
 * └── KafkaTemplate → event publishing
 *
 * Pattern: Facade over pipeline steps (keeps Consumer clean).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEngineService {

        private final RiskCalculationService riskCalculationService;
        private final RedisCacheService redisCacheService;
        private final RiskProfileRepository riskProfileRepository;
        private final KafkaTemplate<String, RiskScoredEvent> kafkaTemplate;

        @Value("${kafka.topics.risk-scored:risk.scored}")
        private String riskScoredTopic;

        /**
         * Full risk evaluation pipeline for a single transaction event.
         *
         * @param event the TransactionCreatedEvent consumed from Kafka
         */
        public void evaluate(TransactionCreatedEvent event) {
                // Step 1: Fetch behavioural context from Redis
                RiskContext context = new RiskContext(
                                redisCacheService.getRecentFraudCount(event.getUserId()),
                                redisCacheService.getRecentTxnCount(event.getUserId()));

                // Step 2: Compute composite risk score (Strategy pattern)
                double riskScore = riskCalculationService.calculateRiskScore(event, context);
                String riskLevel = riskCalculationService.deriveRiskLevel(riskScore);

                // Step 3: Update Redis
                redisCacheService.cacheRiskScore(event.getUserId(), riskScore, riskLevel);
                if ("HIGH".equals(riskLevel)) {
                        redisCacheService.addToHotList(event.getTransactionId());
                }

                // Step 4: Upsert RiskProfile in DB
                upsertRiskProfile(event, riskScore, riskLevel, context);

                // Step 5: Publish RiskScoredEvent downstream
                RiskScoredEvent scoredEvent = RiskScoredEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .transactionId(event.getTransactionId())
                                .userId(event.getUserId())
                                .riskScore(riskScore)
                                .riskLevel(riskLevel)
                                .scoredAt(LocalDateTime.now())
                                .build();

                kafkaTemplate.send(riskScoredTopic, event.getUserId(), scoredEvent);
                log.info("RiskScoredEvent published: txnId={}, score={}, level={}",
                                event.getTransactionId(), String.format("%.4f", riskScore), riskLevel);
        }

        private void upsertRiskProfile(TransactionCreatedEvent event,
                        double riskScore, String riskLevel,
                        RiskContext context) {
                Optional<RiskProfile> existing = riskProfileRepository.findByUserId(event.getUserId());
                RiskProfile profile = existing.orElse(
                                RiskProfile.builder().userId(event.getUserId()).build());
                profile.setRiskScore(riskScore);
                profile.setRiskLevel(riskLevel);
                profile.setRecentFraudCount(context.getRecentFraudCount());
                profile.setTxnFrequency(context.getTxnFrequency());
                riskProfileRepository.save(profile);
        }
}
