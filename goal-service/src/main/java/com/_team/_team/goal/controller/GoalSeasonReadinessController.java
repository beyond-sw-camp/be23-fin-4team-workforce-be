package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.resdto.GoalSeasonReadinessResDto;
import com._team._team.goal.service.GoalSeasonReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/goal")
public class GoalSeasonReadinessController {

    private final GoalSeasonReadinessService goalSeasonReadinessService;

    public GoalSeasonReadinessController(GoalSeasonReadinessService goalSeasonReadinessService) {
        this.goalSeasonReadinessService = goalSeasonReadinessService;
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/seasons/{seasonId}/readiness")
    public ApiResponse<GoalSeasonReadinessResDto> inspect(
            @PathVariable UUID seasonId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalSeasonReadinessService.inspect(seasonId, UUID.fromString(companyId)),
                "평가 시즌 목표 점검 조회 성공"
        );
    }
}
