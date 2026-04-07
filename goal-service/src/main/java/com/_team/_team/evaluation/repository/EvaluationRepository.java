package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.Evaluation;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByEvaluateeIdAndEvaluationPolicy_EvaluationPolicyId(UUID evaluateeId, UUID policyId);

    List<Evaluation> findByEvaluatorId(UUID evaluatorId);

    List<Evaluation> findByEvaluatorIdAndStatus(UUID evaluatorId, EvaluationStatus status);

    List<Evaluation> findByEvaluationPolicy_EvaluationPolicyId(UUID policyId);
}
