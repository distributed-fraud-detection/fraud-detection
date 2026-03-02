package com.frauddetection.frauddecision.service;

import com.frauddetection.common.dto.FraudCaseDTO;
import com.frauddetection.common.exception.ResourceNotFoundException;
import com.frauddetection.frauddecision.entity.FraudCase;
import com.frauddetection.frauddecision.repository.FraudCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudCaseServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @InjectMocks
    private FraudCaseService fraudCaseService;

    private FraudCase sampleCase(String caseId) {
        return FraudCase.builder()
                .caseId(caseId)
                .transactionId("txn-1")
                .userId("user-1")
                .userName("Alice")
                .riskScore(0.72)
                .decision(FraudCase.Decision.REVIEW)
                .status(FraudCase.CaseStatus.PENDING)
                .flagReason("Velocity spike")
                .build();
    }

    @Test
    @DisplayName("getAllCases: maps repository page to DTO page")
    void getAllCases_mapsToDTOPage() {
        when(fraudCaseRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleCase("case-1"))));

        Page<FraudCaseDTO> result = fraudCaseService.getAllCases(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getCaseId()).isEqualTo("case-1");
        assertThat(result.getContent().getFirst().getDecision()).isEqualTo("REVIEW");
    }

    @Test
    @DisplayName("getCase: throws ResourceNotFoundException when missing")
    void getCase_missing_throws() {
        when(fraudCaseRepository.findByCaseId("missing-case")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraudCaseService.getCase("missing-case"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FraudCase");
    }

    @Test
    @DisplayName("reviewCase: APPROVE updates status and returns DTO")
    void reviewCase_approve_updatesStatus() {
        FraudCase pending = sampleCase("case-10");
        when(fraudCaseRepository.findByCaseId("case-10")).thenReturn(Optional.of(pending));
        when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudCaseDTO result = fraudCaseService.reviewCase("case-10", "APPROVE");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(fraudCaseRepository).save(pending);
    }

    @Test
    @DisplayName("reviewCase: invalid action throws and does not persist")
    void reviewCase_invalidAction_throws() {
        FraudCase pending = sampleCase("case-11");
        when(fraudCaseRepository.findByCaseId("case-11")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> fraudCaseService.reviewCase("case-11", "IGNORE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid action");

        verify(fraudCaseRepository, never()).save(any());
    }


    @Test
    @DisplayName("reviewCase: REJECT updates status and returns DTO")
    void reviewCase_reject_updatesStatus() {
        FraudCase pending = sampleCase("case-12");
        when(fraudCaseRepository.findByCaseId("case-12")).thenReturn(Optional.of(pending));
        when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudCaseDTO result = fraudCaseService.reviewCase("case-12", "REJECT");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(fraudCaseRepository).save(pending);
    }

}
