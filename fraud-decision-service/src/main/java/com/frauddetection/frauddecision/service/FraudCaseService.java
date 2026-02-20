package com.frauddetection.frauddecision.service;

import com.frauddetection.common.dto.FraudCaseDTO;
import com.frauddetection.common.exception.ResourceNotFoundException;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for FraudCase CRUD and analyst review operations.
 *
 * SOLID Fix: SRP + DIP
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: FraudCaseController directly injected FraudCaseRepository — the
 * controller was doing service-layer work (business logic, DTO mapping).
 * This is a Controller → Repository anti-pattern.
 *
 * AFTER: Proper layering:
 * Controller → FraudCaseService → FraudCaseRepository
 *
 * The mapping logic (toDTO) is also extracted here from the controller,
 * keeping the controller focused purely on HTTP concerns.
 */
@Service
@RequiredArgsConstructor
public class FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;

    public Page<FraudCaseDTO> getAllCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fraudCaseRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    public FraudCaseDTO getCase(String caseId) {
        return fraudCaseRepository.findByCaseId(caseId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("FraudCase", caseId));
    }

    @Transactional
    public FraudCaseDTO reviewCase(String caseId, String action) {
        FraudCase fraudCase = fraudCaseRepository.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudCase", caseId));

        switch (action.toUpperCase()) {
            case "APPROVE" -> fraudCase.setStatus(FraudCase.CaseStatus.APPROVED);
            case "REJECT" -> fraudCase.setStatus(FraudCase.CaseStatus.REJECTED);
            default -> throw new IllegalArgumentException(
                    "Invalid action: " + action + ". Use APPROVE or REJECT.");
        }

        return toDTO(fraudCaseRepository.save(fraudCase));
    }

    private FraudCaseDTO toDTO(FraudCase fc) {
        return FraudCaseDTO.builder()
                .caseId(fc.getCaseId())
                .transactionId(fc.getTransactionId())
                .userId(fc.getUserId())
                .riskScore(fc.getRiskScore())
                .decision(fc.getDecision().name())
                .status(fc.getStatus().name())
                .flagReason(fc.getFlagReason())
                .createdAt(fc.getCreatedAt())
                .updatedAt(fc.getUpdatedAt())
                .build();
    }
}
