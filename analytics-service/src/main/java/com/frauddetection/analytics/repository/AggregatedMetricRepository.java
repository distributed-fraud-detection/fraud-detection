package com.frauddetection.analytics.repository;

import com.frauddetection.analytics.entity.AggregatedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedMetricRepository extends JpaRepository<AggregatedMetric, Long> {

    Optional<AggregatedMetric> findByMetricDate(LocalDate date);

    @Query("SELECT a FROM AggregatedMetric a ORDER BY a.metricDate DESC")
    List<AggregatedMetric> findRecentMetrics(org.springframework.data.domain.Pageable pageable);
}
