package com._team._team.goal.service;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.dto.resdto.GoalViewMapper;
import com._team._team.goal.dto.resdto.GoalViewResDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoalTreeService {

    public List<GoalViewResDto> decorate(List<Goal> goals) {
        List<GoalViewResDto> result = new ArrayList<>();
        for (Goal goal : goals) {
            result.add(GoalViewMapper.toView(goal));
        }
        return result;
    }
}
