package com.frauddetection.frauddecision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.dto.FraudCaseDTO;
import com.frauddetection.frauddecision.dto.ReviewRequest;
import com.frauddetection.frauddecision.service.FraudCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Layer 2: REST API slice test for FraudCaseController.
 *
 * Spring Boot 4: @WebMvcTest and @MockBean (from boot.test.mock.mockito) were
 * removed.
 * Using Mockito standaloneSetup + @Mock/@InjectMocks approach instead.
 * Pure unit test — no Spring context, extremely fast.
 *
 * Tests cover: GET all (paginated), GET by ID, PUT review with APPROVE/REJECT
 * actions.
 */
@ExtendWith(MockitoExtension.class)
class FraudCaseControllerTest {

        @Mock
        private FraudCaseService fraudCaseService;

        @InjectMocks
        private FraudCaseController fraudCaseController;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(fraudCaseController)
                                .build();
        }

        private FraudCaseDTO sampleCase(String caseId, String decision) {
                FraudCaseDTO dto = new FraudCaseDTO();
                dto.setCaseId(caseId);
                dto.setUserId("u001");
                dto.setTransactionId("tx-001");
                dto.setRiskScore(0.88);
                dto.setDecision(decision);
                return dto;
        }

    @Test
    @DisplayName("GET /api/fraud-cases → 200 with paginated list")
    void getAllCases_returns200() throws Exception {
        when(fraudCaseService.getAllCases(0, 20))
                .thenReturn(new PageImpl<>(List.of(sampleCase("case-1", "BLOCK"))));

        mockMvc.perform(get("/api/fraud-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].caseId").value("case-1"))
                .andExpect(jsonPath("$.content[0].decision").value("BLOCK"));
    }

    @Test
    @DisplayName("GET /api/fraud-cases/{id} → 200 with case details")
    void getCase_found_returns200() throws Exception {
        when(fraudCaseService.getCase("case-42"))
                .thenReturn(sampleCase("case-42", "REVIEW"));

        mockMvc.perform(get("/api/fraud-cases/case-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value("case-42"))
                .andExpect(jsonPath("$.decision").value("REVIEW"));
    }

        @Test
        @DisplayName("PUT /api/fraud-cases/{id}/review with APPROVE → 200")
        void reviewCase_approve_returns200() throws Exception {
                FraudCaseDTO approved = sampleCase("case-10", "APPROVE");
                when(fraudCaseService.reviewCase("case-10", "APPROVE")).thenReturn(approved);

                ReviewRequest req = new ReviewRequest();
                req.setAction("APPROVE");

                mockMvc.perform(put("/api/fraud-cases/case-10/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.decision").value("APPROVE"));
        }

        @Test
        @DisplayName("PUT /api/fraud-cases/{id}/review with REJECT → 200")
        void reviewCase_reject_returns200() throws Exception {
                FraudCaseDTO rejected = sampleCase("case-11", "REJECT");
                when(fraudCaseService.reviewCase("case-11", "REJECT")).thenReturn(rejected);

                ReviewRequest req = new ReviewRequest();
                req.setAction("REJECT");

                mockMvc.perform(put("/api/fraud-cases/case-11/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.decision").value("REJECT"));
        }

        @Test
        @DisplayName("PUT /api/fraud-cases/{id}/review with invalid action → 400")
        void reviewCase_invalidAction_returns400() throws Exception {
                String badBody = """
                                { "action": "EXPLODE" }
                                """;

                mockMvc.perform(put("/api/fraud-cases/case-99/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badBody))
                                .andExpect(status().isBadRequest());
        }
}
