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

    List<Goal> findByKpiTemplate_KpiTemplateIdAndStatus(UUID kpiTemplateId, GoalStatus status);
}
