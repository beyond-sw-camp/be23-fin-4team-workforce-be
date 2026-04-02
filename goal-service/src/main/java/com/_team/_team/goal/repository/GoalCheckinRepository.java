package com._team._team.goal.repository;

import com._team._team.goal.domain.GoalCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoalCheckinRepository extends JpaRepository<GoalCheckin, UUID> {
}
