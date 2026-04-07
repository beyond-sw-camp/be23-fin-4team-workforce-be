package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.CalibrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalibrationLogRepository extends JpaRepository<CalibrationLog, UUID> {

    List<CalibrationLog> findByEvaluation_EvaluationIdOrderByAdjustedAtDesc(UUID evaluationId);
}
