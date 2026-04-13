package com._team._team.goal.service;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.dto.resdto.GoalResDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoalTreeService {

    private static final int MAX_DEPTH = 32;

    public List<GoalResDto> decorate(List<Goal> goals) {
        Map<UUID, Goal> byId = new HashMap<>();
        Map<UUID, List<Goal>> childrenMap = new HashMap<>();
        for (Goal goal : goals) {
            byId.put(goal.getGoalId(), goal);
            if (goal.getParentGoal() != null) {
                childrenMap.computeIfAbsent(goal.getParentGoal().getGoalId(), key -> new ArrayList<>()).add(goal);
            }
        }

        List<GoalResDto> result = new ArrayList<>();
        for (Goal goal : goals) {
            int depth = getDepth(goal, byId);
            List<UUID> path = buildPath(goal, byId);
            int childCount = childrenMap.getOrDefault(goal.getGoalId(), List.of()).size();
            GoalResDto dto = GoalResDto.from(goal);
            dto.setDepth(depth);
            dto.setPath(path);
            dto.setChildCount(childCount);
            dto.setHasChildren(childCount > 0);
            result.add(dto);
        }
        return result;
    }

    private int getDepth(Goal goal, Map<UUID, Goal> byId) {
        Set<UUID> visited = new HashSet<>();
        visited.add(goal.getGoalId());
        int depth = 0;
        Goal cursor = goal;
        while (cursor.getParentGoal() != null && depth < MAX_DEPTH) {
            UUID parentId = cursor.getParentGoal().getGoalId();
            if (!visited.add(parentId)) {
                break; // 사이클 감지 — 중단
            }
            depth++;
            cursor = byId.get(parentId);
            if (cursor == null) break;
        }
        return depth;
    }

    private List<UUID> buildPath(Goal goal, Map<UUID, Goal> byId) {
        LinkedList<UUID> path = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();
        Goal cursor = goal;
        path.addFirst(cursor.getGoalId());
        visited.add(cursor.getGoalId());
        int steps = 0;
        while (cursor.getParentGoal() != null && steps < MAX_DEPTH) {
            UUID parentId = cursor.getParentGoal().getGoalId();
            if (!visited.add(parentId)) {
                break; // 사이클 감지
            }
            Goal parent = byId.get(parentId);
            if (parent == null) {
                path.addFirst(parentId);
                break;
            }
            path.addFirst(parent.getGoalId());
            cursor = parent;
            steps++;
        }
        return path;
    }
}
