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

    /** 목표 트리 최대 허용 깊이 — 이상 깊이는 데이터 이상으로 판단. */
    private static final int MAX_DEPTH = 32;

    private final GoalRepository goalRepository;

    public GoalRollupService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    /**
     * 해당 목표 및 모든 조상 목표들의 rolled achievement 를 재계산
     */
    public void recalculateFrom(UUID goalId) {
        Goal start = goalRepository.findById(goalId).orElse(null);
        if (start == null) return;

        Set<UUID> visited = new HashSet<>();
        recalculateSubtree(start, visited, 0);

        // 부모 방향으로도 올라가며 재계산. 사이클 방어.
        Set<UUID> parentVisited = new HashSet<>();
        parentVisited.add(start.getGoalId());
        Goal cursor = start.getParentGoal();
        int steps = 0;
        while (cursor != null && steps < MAX_DEPTH) {
            if (!parentVisited.add(cursor.getGoalId())) {
                break; // 사이클 감지
            }
            // 부모의 서브트리 재계산 (이미 계산된 노드는 visited 로 스킵)
            recalculateSubtree(cursor, new HashSet<>(), 0);
            cursor = cursor.getParentGoal();
            steps++;
        }
    }

    /**
     * 외부에서 현재 상태를 조회할 때 사용. 저장은 하지 않음.
     */
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

    /**
     * goal 을 루트로 하는 서브트리를 재계산하여 영속화.
     *
     * @return 재계산된 rolled 값
     */
    private BigDecimal recalculateSubtree(Goal goal, Set<UUID> visited, int depth) {
        if (depth > MAX_DEPTH) {
            // 깊이 초과 — 더 이상 파고들지 않음. 자기 값 반환.
            return goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
        }
        if (!visited.add(goal.getGoalId())) {
            // 사이클 감지 — 현재 값 그대로 반환.
            return goal.getRolledAchievementPct() == null ? BigDecimal.ZERO : goal.getRolledAchievementPct();
        }

        // 자식 목표 직접 조회 (회사 전체 스캔 제거)
        List<Goal> allChildren = goalRepository.findByParentGoal_GoalId(goal.getGoalId());
        List<Goal> included = allChildren.stream()
                .filter(this::isRollupChildIncluded)
                .toList();

        if (included.isEmpty()) {
            BigDecimal self = goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
            goal.updateRollup(self, RollupSource.SELF);
            return self;
        }

        // 먼저 자식들을 재귀 재계산
        for (Goal child : included) {
            recalculateSubtree(child, visited, depth + 1);
        }

        // 자동 집계가 꺼진 목표는 자기 값 유지
        if (!goal.isAutoUpdate()) {
            BigDecimal self = goal.getAchievementPct() == null ? BigDecimal.ZERO : goal.getAchievementPct();
            goal.updateRollup(self, RollupSource.SELF);
            return self;
        }

        BigDecimal rolled;
        RollupSource source;
        if (goal.getRollupPolicy() == RollupPolicy.CHILDREN_WEIGHTED) {
            rolled = weighted(included);
            source = RollupSource.CHILDREN_WEIGHTED;
        } else {
            List<BigDecimal> values = included.stream()
                    .map(c -> c.getRolledAchievementPct() == null ? BigDecimal.ZERO : c.getRolledAchievementPct())
                    .toList();
            rolled = avgByValues(values);
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

    // 집계 포함 여부 : CANCELLED는 제외, DRAFT는 아직 승인, 활성 이전이므로 달성률 오염시키지 않도록 제외
    private boolean isRollupChildIncluded(Goal child) {
        GoalStatus status = child.getStatus();
        return status != GoalStatus.CANCELLED && status != GoalStatus.DRAFT;
    }
}
