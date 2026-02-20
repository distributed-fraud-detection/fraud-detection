package com.frauddetection.transaction.controller;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.transaction.service.ITransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final ITransactionService transactionService; // DIP: interface, not concrete class

    /**
     * Submit a new transaction for fraud screening.
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @Valid @RequestBody TransactionDTO request) {
        TransactionDTO created = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieve a transaction by its unique transactionId.
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    /**
     * Get all transactions for a given user (most recent first).
     * GET /api/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDTO>> getUserTransactions(
            @PathVariable String userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }
}
