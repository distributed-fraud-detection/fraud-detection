package com.frauddetection.transaction.service;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.common.exception.ResourceNotFoundException;
import com.frauddetection.transaction.entity.Transaction;
import com.frauddetection.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core transaction service — pure orchestration, no cross-cutting concerns.
 *
 * SOLID Fixes Applied:
 * ─────────────────────────────────────────────────────────────────────────────
 * SRP: Each responsibility extracted to its own class:
 * ┌─ RateLimitService ← "is the user allowed to transact?"
 * ├─ TransactionMapper ← "how do I convert between DTO/entity/event?"
 * └─ TransactionService ← "orchestrate: validate → save → publish"
 *
 * DIP: Implements ITransactionService interface so controllers and tests
 * depend on the abstraction, not this concrete class.
 *
 * OCP: Rate-limit errors are now a typed RateLimitExceededException,
 * handled in GlobalExceptionHandler without touching this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    private final RateLimitService rateLimitService;
    private final TransactionMapper mapper;

    @Value("${kafka.topics.transactions-created:transactions.created}")
    private String transactionsTopic;

    /**
     * Create and publish a new transaction.
     * Delegates rate-limiting to RateLimitService and mapping to TransactionMapper.
     */
    @Override
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO request) {
        // 1. Enforce rate limit (throws RateLimitExceededException → HTTP 429)
        rateLimitService.checkRateLimit(request.getUserId());

        // 2. Persist
        Transaction saved = transactionRepository.save(mapper.toEntity(request));
        log.info("Transaction saved: id={}, userId={}", saved.getTransactionId(), saved.getUserId());

        // 3. Publish event
        TransactionCreatedEvent event = mapper.toEvent(saved);
        kafkaTemplate.send(transactionsTopic, saved.getUserId(), event);
        log.info("Published TransactionCreatedEvent: txnId={}", saved.getTransactionId());

        return mapper.toDTO(saved);
    }

    @Override
    public TransactionDTO getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
    }

    @Override
    public List<TransactionDTO> getTransactionsByUser(String userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
}
