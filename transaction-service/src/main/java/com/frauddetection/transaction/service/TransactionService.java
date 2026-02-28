package com.frauddetection.transaction.service;

import com.frauddetection.common.dto.TransactionDTO;

import java.util.List;

/**
 * Service interface for transaction operations.
 *
 * SOLID Fix: DIP (Dependency Inversion Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: TransactionController depended directly on the concrete
 * TransactionService class, making it impossible to swap
 * implementations (e.g., a mock in tests) without changing Controller.
 *
 * AFTER: Controller depends on this interface. The concrete
 * TransactionService is injected by Spring — true DIP.
 *
 * Secondary benefit: this is the contract consumers code against,
 * keeping the public API surface explicit and documented.
 */
public interface TransactionService {

    /**
     * Validate, persist, and publish a new transaction for fraud screening.
     * Enforces per-user rate limiting (10 txn/min) via Redis.
     *
     * @throws com.frauddetection.common.exception.RateLimitExceededException if
     *                                                                        rate
     *                                                                        limit
     *                                                                        hit
     */
    TransactionDTO createTransaction(TransactionDTO request);

    /**
     * Retrieve a single transaction by its business-key transactionId.
     *
     * @throws com.frauddetection.common.exception.ResourceNotFoundException if not
     *                                                                       found
     */
    TransactionDTO getTransaction(String transactionId);

    /**
     * Return all transactions for a given user, ordered newest-first.
     */
    List<TransactionDTO> getTransactionsByUser(String userId);
}
