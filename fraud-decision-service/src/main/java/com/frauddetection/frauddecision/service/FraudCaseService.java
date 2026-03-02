package com.frauddetection.frauddecision.service;

import com.frauddetection.common.dto.FraudCaseDTO;
import org.springframework.data.domain.Page;

public interface FraudCaseService {

    Page<FraudCaseDTO> getAllCases(int page, int size);

    FraudCaseDTO getCase(String caseId);

    FraudCaseDTO reviewCase(String caseId, String action);
}
