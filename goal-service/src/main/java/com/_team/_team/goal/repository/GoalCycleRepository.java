package com._team._team.goal.repository;

import com._team._team.goal.domain.GoalCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoalCycleRepository extends JpaRepository<GoalCycle, UUID> {
}
