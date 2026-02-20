package com.frauddetection.frauddecision.controller;

import com.frauddetection.common.dto.FraudCaseDTO;
import com.frauddetection.frauddecision.dto.ReviewRequest;
import com.frauddetection.frauddecision.service.FraudCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for fraud case management.
 *
 * SOLID Fixes Applied:
 * ─────────────────────────────────────────────────────────────────────────────
 * SRP: Controller's only job is HTTP request/response handling.
 * Business logic, DB queries, and DTO mapping moved to FraudCaseService.
 *
 * DIP: Depends on FraudCaseService (could be an interface), not on
 * FraudCaseRepository directly.
 *
 * Type-safe review request: Map<String, String> replaced with @Valid
 * ReviewRequest.
 * Invalid actions are rejected at the deserialisation/validation layer (HTTP
 * 400)
 * before reaching any business logic.
 */
@RestController
@RequestMapping("/api/fraud-cases")
@RequiredArgsConstructor
public class FraudCaseController {

    private final FraudCaseService fraudCaseService; // DIP: service layer, not repo

    /**
     * GET /api/fraud-cases?page=0&size=20
     * Returns paginated fraud cases, newest first.
     */
    @GetMapping
    public ResponseEntity<Page<FraudCaseDTO>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(fraudCaseService.getAllCases(page, size));
    }

    /**
     * GET /api/fraud-cases/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FraudCaseDTO> getCase(@PathVariable String id) {
        return ResponseEntity.ok(fraudCaseService.getCase(id));
    }

    /**
     * PUT /api/fraud-cases/{id}/review
     * Body: { "action": "APPROVE" | "REJECT" }
     * 
     * @Valid enforces the pattern before the method body runs.
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<FraudCaseDTO> reviewCase(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(fraudCaseService.reviewCase(id, request.getAction()));
    }
}
