package com.frauddetection.transaction.repository;

import com.frauddetection.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByUserIdOrderByTimestampDesc(String userId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.timestamp >= :since")
    long countRecentTransactions(@Param("userId") String userId,
                                  @Param("since") LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.timestamp DESC")
    List<Transaction> findRecentByUserId(@Param("userId") String userId);
}
