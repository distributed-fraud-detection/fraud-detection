package com.frauddetection.transaction.service;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps between Transaction entity and TransactionDTO / events.
 *
 * SOLID Fix: SRP (Single Responsibility Principle)
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: The toDTO() conversion method lived inside TransactionService,
 * giving the service a second responsibility (data mapping).
 *
 * AFTER: ALL entity ↔ DTO and entity ↔ event conversions live here.
 * TransactionService calls this mapper — it doesn't know about DTO fields.
 *
 * Pattern: Data Mapper (P of EAA — Fowler)
 */
@Component
public class TransactionMapper {

    public Transaction toEntity(TransactionDTO dto) {
        return Transaction.builder()
                .userId(dto.getUserId())
                .amount(dto.getAmount())
                .location(dto.getLocation())
                .merchantType(dto.getMerchantType())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }

    public TransactionDTO toDTO(Transaction t) {
        return TransactionDTO.builder()
                .transactionId(t.getTransactionId())
                .userId(t.getUserId())
                .amount(t.getAmount())
                .location(t.getLocation())
                .merchantType(t.getMerchantType())
                .status(t.getStatus().name())
                .timestamp(t.getTimestamp())
                .build();
    }

    public TransactionCreatedEvent toEvent(Transaction saved) {
        return TransactionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(saved.getTransactionId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .location(saved.getLocation())
                .merchantType(saved.getMerchantType())
                .timestamp(saved.getTimestamp())
                .build();
    }
}
