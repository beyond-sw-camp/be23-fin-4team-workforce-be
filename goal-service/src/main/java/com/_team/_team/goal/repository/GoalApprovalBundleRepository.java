package com._team._team.goal.repository;

import com._team._team.goal.domain.GoalApprovalBundle;
import com._team._team.goal.domain.enums.BundleApprovalStatus;
import com._team._team.goal.domain.enums.GoalApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalApprovalBundleRepository extends JpaRepository<GoalApprovalBundle, UUID> {

    List<GoalApprovalBundle> findByCompanyIdAndStatusOrderByRequestedAtDesc(UUID companyId, BundleApprovalStatus status);

    List<GoalApprovalBundle> findByCompanyIdAndStatusAndDecisionAndApproverIdOrderByRequestedAtDesc(
            UUID companyId, BundleApprovalStatus status, GoalApprovalDecision decision, UUID approverId);

    List<GoalApprovalBundle> findByCompanyIdAndStatusAndDecisionAndRequestedByOrderByRequestedAtDesc(
            UUID companyId, BundleApprovalStatus status, GoalApprovalDecision decision, UUID requestedBy);

    List<GoalApprovalBundle> findByCompanyIdAndApproverIdOrderByRequestedAtDesc(UUID companyId, UUID approverId);

    List<GoalApprovalBundle> findByCompanyIdAndRequestedByOrderByRequestedAtDesc(UUID companyId, UUID requestedBy);
}
