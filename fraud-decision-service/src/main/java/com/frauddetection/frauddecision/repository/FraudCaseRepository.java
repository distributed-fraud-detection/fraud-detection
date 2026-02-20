package com.frauddetection.frauddecision.repository;

import com.frauddetection.frauddecision.entity.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    Optional<FraudCase> findByCaseId(String caseId);

    Optional<FraudCase> findByTransactionId(String transactionId);

    Page<FraudCase> findByStatusOrderByCreatedAtDesc(FraudCase.CaseStatus status, Pageable pageable);

    Page<FraudCase> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
