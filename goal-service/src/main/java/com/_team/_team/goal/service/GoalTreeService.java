package com._team._team.goal.service;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.dto.resdto.GoalResDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoalTreeService {

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
        int depth = 0;
        Goal cursor = goal;
        while (cursor.getParentGoal() != null) {
            depth++;
            cursor = byId.get(cursor.getParentGoal().getGoalId());
            if (cursor == null) break;
        }
        return depth;
    }

    private List<UUID> buildPath(Goal goal, Map<UUID, Goal> byId) {
        LinkedList<UUID> path = new LinkedList<>();
        Goal cursor = goal;
        path.addFirst(cursor.getGoalId());
        while (cursor.getParentGoal() != null) {
            Goal parent = byId.get(cursor.getParentGoal().getGoalId());
            if (parent == null) {
                path.addFirst(cursor.getParentGoal().getGoalId());
                break;
            }
            path.addFirst(parent.getGoalId());
            cursor = parent;
        }
        return path;
    }
}
