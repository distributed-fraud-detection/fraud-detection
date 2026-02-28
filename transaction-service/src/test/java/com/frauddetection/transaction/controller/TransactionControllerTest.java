package com.frauddetection.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.exception.RateLimitExceededException;
import com.frauddetection.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Layer 2: REST API slice test for TransactionController.
 *
 * Spring Boot 4: @WebMvcTest was removed — using @SpringBootTest
 * + @AutoConfigureMockMvc
 * with @MockitoBean (replaces old @MockBean) for the service layer.
 * The service layer is mocked — no DB, no Kafka, no Redis needed at the
 * container level
 * (test profile disables auto-configuration for those).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    private TransactionDTO sampleDTO() {
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId("u001");
        dto.setAmount(BigDecimal.valueOf(500));
        dto.setLocation("Mumbai");
        dto.setMerchantType("E-Commerce");
        return dto;
    }

    @Test
    @DisplayName("POST /api/transactions → 201 Created with TransactionDTO body")
    void createTransaction_returns201() throws Exception {
        TransactionDTO result = sampleDTO();
        result.setTransactionId("tx-generated-id");

        when(transactionService.createTransaction(any(TransactionDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-generated-id"))
                .andExpect(jsonPath("$.userId").value("u001"));
    }

    @Test
    @DisplayName("POST /api/transactions → 429 when rate limit exceeded")
    void createTransaction_rateLimitExceeded_returns429() throws Exception {
        when(transactionService.createTransaction(any()))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded for user: u001"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDTO())))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("GET /api/transactions/{id} → 200 with transaction body")
    void getTransaction_found_returns200() throws Exception {
        TransactionDTO dto = sampleDTO();
        dto.setTransactionId("tx-123");
        when(transactionService.getTransaction("tx-123")).thenReturn(dto);

        mockMvc.perform(get("/api/transactions/tx-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-123"))
                .andExpect(jsonPath("$.userId").value("u001"));
    }

    @Test
    @DisplayName("GET /api/transactions/user/{userId} → 200 with list")
    void getUserTransactions_returns200() throws Exception {
        when(transactionService.getTransactionsByUser("u001")).thenReturn(List.of(sampleDTO()));

        mockMvc.perform(get("/api/transactions/user/u001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u001"));
    }

    @Test
    @DisplayName("POST /api/transactions with missing required field → 400 Bad Request")
    void createTransaction_invalidBody_returns400() throws Exception {
        String badJson = """
                { "amount": 500, "location": "Delhi", "merchantType": "ATM" }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest());
    }
}
