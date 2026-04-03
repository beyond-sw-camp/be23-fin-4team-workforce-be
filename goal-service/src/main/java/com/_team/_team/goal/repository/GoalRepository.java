package com._team._team.goal.repository;

import com._team._team.goal.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
