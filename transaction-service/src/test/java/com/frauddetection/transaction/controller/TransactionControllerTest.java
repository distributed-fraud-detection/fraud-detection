package com.frauddetection.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.exception.RateLimitExceededException;
import com.frauddetection.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Layer 2: Unit-level MockMvc test for TransactionController.
 *
 * Spring Boot 4 removed @WebMvcTest from the web.servlet autoconfigure package.
 * We use MockMvcBuilders.standaloneSetup() instead — loads ONLY the controller,
 * filters and advices. Service is mocked via Mockito. No Spring context needed.
 * Fast: ~2-3 seconds.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(transactionController)
                .build();
    }

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
                .thenThrow(new RateLimitExceededException("u001", 10));

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
}
