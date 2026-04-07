package com._team._team.goal.repository;

import com._team._team.goal.domain.PerformanceRecord;
import com._team._team.goal.domain.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceRecordRepository extends JpaRepository<PerformanceRecord, UUID> {

    List<PerformanceRecord> findByGoal_GoalIdOrderByCreatedAtDesc(UUID goalId);

    List<PerformanceRecord> findByGoal_GoalIdAndReviewStatus(UUID goalId, ReviewStatus reviewStatus);
}
