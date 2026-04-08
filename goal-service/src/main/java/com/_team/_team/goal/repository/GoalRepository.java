package com._team._team.goal.repository;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<Goal> findByCompanyId(UUID companyId);
    List<Goal> findByCompanyIdAndOwnerId(UUID companyId, UUID ownerId);
    List<Goal> findByCompanyIdAndParentGoalIsNull(UUID companyId);
    List<Goal> findByParentGoal_GoalId(UUID parentGoalId);
    long countByParentGoal_GoalId(UUID parentGoalId);

    List<Goal> findByKpiTemplate_KpiTemplateIdAndStatus(UUID kpiTemplateId, GoalStatus status);
}
