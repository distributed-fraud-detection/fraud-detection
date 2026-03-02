package com.frauddetection.transaction.controller;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService; // DIP: interface, not concrete class

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
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable("id") String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    /**
     * Get all transactions for a given user (most recent first).
     * GET /api/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransactionDTO>> getUserTransactions(
            @PathVariable("userId") String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId, page, size));
    }
}
