package com._team._team.goal.service;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.RollupPolicy;
import com._team._team.goal.domain.enums.RollupSource;
import com._team._team.goal.repository.GoalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class GoalRollupService {

    private final GoalRepository goalRepository;

    public GoalRollupService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public void recalculateFrom(UUID goalId) {
        Goal start = goalRepository.findById(goalId).orElse(null);
        if (start == null) return;
        recalculateRecursively(start);
        Goal cursor = start.getParentGoal();
        while (cursor != null) {
            recalculateRecursively(cursor);
            cursor = cursor.getParentGoal();
        }
    }

    public BigDecimal calculateRolledAchievement(Goal goal, Map<UUID, List<Goal>> childrenMap) {
        List<Goal> children = childrenMap.getOrDefault(goal.getGoalId(), List.of())
                .stream()
                .filter(this::isRollupChildIncluded)
                .toList();
        if (children.isEmpty()) {
            return goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
        }
        if (goal.getRollupPolicy() == RollupPolicy.CHILDREN_WEIGHTED) {
            return weighted(children);
        }
        return avg(children);
    }

    private void recalculateRecursively(Goal goal) {
        Map<UUID, List<Goal>> childrenMap = new HashMap<>();
        List<Goal> companyGoals = goalRepository.findByCompanyId(goal.getCompanyId());
        for (Goal g : companyGoals) {
            if (g.getParentGoal() != null) {
                childrenMap.computeIfAbsent(g.getParentGoal().getGoalId(), key -> new ArrayList<>()).add(g);
            }
        }
        calculateAndPersist(goal, childrenMap);
    }

    private BigDecimal calculateAndPersist(Goal goal, Map<UUID, List<Goal>> childrenMap) {
        List<Goal> children = childrenMap.getOrDefault(goal.getGoalId(), List.of())
                .stream()
                .filter(this::isRollupChildIncluded)
                .toList();
        if (children.isEmpty()) {
            BigDecimal self = goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
            goal.updateRollup(self, RollupSource.SELF);
            return self;
        }

        for (Goal child : children) {
            calculateAndPersist(child, childrenMap);
        }

        if (!goal.isAutoUpdate()) {
            BigDecimal self = goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
            goal.updateRollup(self, RollupSource.SELF);
            return self;
        }

        List<BigDecimal> childValues = new ArrayList<>();
        for (Goal child : children) {
            BigDecimal cv = child.getRolledAchievementPct() == null ? BigDecimal.ZERO : child.getRolledAchievementPct();
            childValues.add(cv);
        }

        BigDecimal rolled;
        RollupSource source;
        if (goal.getRollupPolicy() == RollupPolicy.CHILDREN_WEIGHTED) {
            rolled = weighted(children);
            source = RollupSource.CHILDREN_WEIGHTED;
        } else {
            rolled = avgByValues(childValues);
            source = RollupSource.CHILDREN_AVG;
        }
        goal.updateRollup(rolled, source);
        return rolled;
    }

    private BigDecimal avg(List<Goal> children) {
        List<BigDecimal> values = children.stream()
                .map(Goal::getRolledAchievementPct)
                .filter(Objects::nonNull)
                .toList();
        return avgByValues(values);
    }

    private BigDecimal avgByValues(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal weighted(List<Goal> children) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (Goal child : children) {
            BigDecimal value = child.getRolledAchievementPct() == null ? BigDecimal.ZERO : child.getRolledAchievementPct();
            BigDecimal weight = child.getWeightPct() == null ? BigDecimal.ZERO : child.getWeightPct();
            weightedSum = weightedSum.add(value.multiply(weight));
            weightSum = weightSum.add(weight);
        }
        if (weightSum.compareTo(BigDecimal.ZERO) <= 0) {
            return avg(children);
        }
        return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
    }

    private boolean isRollupChildIncluded(Goal child) {
        return child.getStatus() != GoalStatus.CANCELLED;
    }
}
