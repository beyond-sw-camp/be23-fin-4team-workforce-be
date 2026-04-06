package com._team._team.goal.repository;

import com._team._team.goal.domain.PerformanceRecordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceRecordHistoryRepository extends JpaRepository<PerformanceRecordHistory, UUID> {

    List<PerformanceRecordHistory> findByPerformanceRecord_PerformanceRecordIdOrderByChangedAtDesc(UUID performanceRecordId);
}
