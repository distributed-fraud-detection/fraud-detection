package com.frauddetection.riskengine.service.impl;

import com.frauddetection.common.events.RiskScoredEvent;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.riskengine.entity.RiskProfile;
import com.frauddetection.riskengine.repository.RiskProfileRepository;
import com.frauddetection.riskengine.service.RedisCacheService;
import com.frauddetection.riskengine.service.RiskCalculationService;
import com.frauddetection.riskengine.service.RiskContext;
import com.frauddetection.riskengine.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEngineServiceImpl implements RiskEngineService {

    private final RiskCalculationService riskCalculationService;
    private final RedisCacheService redisCacheService;
    private final RiskProfileRepository riskProfileRepository;
    private final KafkaTemplate<String, RiskScoredEvent> kafkaTemplate;

    @Value("${kafka.topics.risk-scored:risk.scored}")
    private String riskScoredTopic;

    @Override
    public void evaluate(TransactionCreatedEvent event) {
        RiskContext context = new RiskContext(
                redisCacheService.getRecentFraudCount(event.getUserId()),
                redisCacheService.getRecentTxnCount(event.getUserId()));

        double riskScore = riskCalculationService.calculateRiskScore(event, context);
        String riskLevel = riskCalculationService.deriveRiskLevel(riskScore);

        redisCacheService.cacheRiskScore(event.getUserId(), riskScore, riskLevel);
        if ("HIGH".equals(riskLevel)) {
            redisCacheService.addToHotList(event.getTransactionId());
        }

        upsertRiskProfile(event, riskScore, riskLevel, context);

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
        RiskProfile profile = existing.orElse(RiskProfile.builder().userId(event.getUserId()).build());
        profile.setRiskScore(riskScore);
        profile.setRiskLevel(riskLevel);
        profile.setRecentFraudCount(context.getRecentFraudCount());
        profile.setTxnFrequency(context.getTxnFrequency());
        riskProfileRepository.save(profile);
    }
}
