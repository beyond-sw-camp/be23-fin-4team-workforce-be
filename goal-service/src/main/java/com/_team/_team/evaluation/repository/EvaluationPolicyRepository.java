package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.EvaluationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationPolicyRepository extends JpaRepository<EvaluationPolicy, UUID> {

    List<EvaluationPolicy> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId);

    List<EvaluationPolicy> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
