package com.sorm.sorm_backend;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricRepository extends JpaRepository<Metric, Long> {

    Optional<Metric> findFirstByDeviceIdOrderByRecordedAtDesc(Integer deviceId);

    List<Metric> findByDeviceIdOrderByRecordedAtDesc(Integer deviceId, Pageable pageable);
}