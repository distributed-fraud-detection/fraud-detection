package com.frauddetection.frauddecision.service.impl;

import com.frauddetection.common.dto.FraudCaseDTO;
import com.frauddetection.common.exception.ResourceNotFoundException;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import com.frauddetection.frauddecision.service.FraudCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FraudCaseServiceImpl implements FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;

    @Override
    public Page<FraudCaseDTO> getAllCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fraudCaseRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    @Override
    public FraudCaseDTO getCase(String caseId) {
        return fraudCaseRepository.findByCaseId(caseId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("FraudCase", caseId));
    }

    @Override
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
                .userName(fc.getUserName())
                .riskScore(fc.getRiskScore())
                .decision(fc.getDecision().name())
                .status(fc.getStatus().name())
                .flagReason(fc.getFlagReason())
                .createdAt(fc.getCreatedAt())
                .updatedAt(fc.getUpdatedAt())
                .build();
    }
}
