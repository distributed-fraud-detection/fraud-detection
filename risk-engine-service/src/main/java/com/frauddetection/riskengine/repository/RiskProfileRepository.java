package com.frauddetection.riskengine.repository;

import com.frauddetection.riskengine.entity.RiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskProfileRepository extends JpaRepository<RiskProfile, Long> {

    Optional<RiskProfile> findByUserId(String userId);

    @Query("SELECT r FROM RiskProfile r ORDER BY r.riskScore DESC")
    List<RiskProfile> findTopRiskProfiles(org.springframework.data.domain.Pageable pageable);
}
