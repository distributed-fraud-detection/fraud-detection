package com.frauddetection.transaction.service;

import com.frauddetection.common.dto.TransactionDTO;
import com.frauddetection.common.events.TransactionCreatedEvent;
import com.frauddetection.common.exception.ResourceNotFoundException;
import com.frauddetection.transaction.entity.Transaction;
import com.frauddetection.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "transactionsTopic", "transactions.created");
    }

    private TransactionDTO dto(String userId) {
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(userId);
        dto.setAmount(BigDecimal.valueOf(1000));
        dto.setLocation("Mumbai");
        dto.setMerchantType("E-Commerce");
        return dto;
    }

    @Test
    @DisplayName("Normal transaction: saved to DB and published to Kafka")
    void normalTransaction_savedAndPublished() {
        TransactionDTO req = dto("u001");
        when(mapper.toEntity(any())).thenReturn(Transaction.builder()
                .userId("u001").amount(BigDecimal.valueOf(1000))
                .location("Mumbai").merchantType("E-Commerce")
                .status(Transaction.TransactionStatus.PENDING).build());
        when(mapper.toDTO(any())).thenReturn(req);
        when(mapper.toEvent(any())).thenReturn(TransactionCreatedEvent.builder()
                .transactionId("tx-test").userId("u001")
                .amount(BigDecimal.valueOf(1000)).location("Mumbai")
                .merchantType("E-Commerce").build());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO result = service.createTransaction(req);

        verify(transactionRepository).save(any(Transaction.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any(TransactionCreatedEvent.class));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Rate limit exceeded: throws exception and does NOT save")
    void rateLimitExceeded_throwsException() {
        doThrow(new RuntimeException("Rate limit exceeded")).when(rateLimitService).checkRateLimit(anyString());

        assertThatThrownBy(() -> service.createTransaction(dto("u001")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rate limit");

        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }


    @Test
    @DisplayName("getTransaction: returns mapped DTO when repository hit")
    void getTransaction_found_returnsDto() {
        Transaction tx = Transaction.builder().transactionId("tx-1").userId("u001").build();
        TransactionDTO dto = dto("u001");
        dto.setTransactionId("tx-1");

        when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));
        when(mapper.toDTO(tx)).thenReturn(dto);

        TransactionDTO result = service.getTransaction("tx-1");

        assertThat(result.getTransactionId()).isEqualTo("tx-1");
    }

    @Test
    @DisplayName("getTransaction: throws ResourceNotFoundException when absent")
    void getTransaction_missing_throws() {
        when(transactionRepository.findByTransactionId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction");
    }

    @Test
    @DisplayName("getTransactionsByUser: maps paged entities to DTOs")
    void getTransactionsByUser_mapsPage() {
        Transaction tx = Transaction.builder().transactionId("tx-2").userId("u001").build();
        TransactionDTO dto = dto("u001");
        dto.setTransactionId("tx-2");

        when(transactionRepository.findByUserIdOrderByTimestampDesc(eq("u001"), any()))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(mapper.toDTO(tx)).thenReturn(dto);

        Page<TransactionDTO> result = service.getTransactionsByUser("u001", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTransactionId()).isEqualTo("tx-2");
    }

}
