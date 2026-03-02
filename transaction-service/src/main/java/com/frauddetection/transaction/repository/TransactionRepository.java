package com.frauddetection.transaction.repository;

import com.frauddetection.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.timestamp >= :since")
    long countRecentTransactions(@Param("userId") String userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.timestamp DESC")
    List<Transaction> findRecentByUserId(@Param("userId") String userId);
}
