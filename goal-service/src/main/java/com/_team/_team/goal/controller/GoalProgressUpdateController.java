package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.GoalProgressUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalProgressUpdateResDto;
import com._team._team.goal.permission.PositionPermissionReader;
import com._team._team.goal.service.GoalProgressUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goal")
public class GoalProgressUpdateController {

    private final GoalProgressUpdateService goalProgressUpdateService;
    private final PositionPermissionReader positionPermissionReader;

    public GoalProgressUpdateController(
            GoalProgressUpdateService goalProgressUpdateService,
            PositionPermissionReader positionPermissionReader) {
        this.goalProgressUpdateService = goalProgressUpdateService;
        this.positionPermissionReader = positionPermissionReader;
    }

    @PostMapping("/{goalId}/updates")
    public ApiResponse<GoalProgressUpdateResDto> addUpdate(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid GoalProgressUpdateReqDto dto,
            HttpServletRequest request) {
        boolean hasGoalUpdatePermission = positionPermissionReader.hasPermission(request, Resource.GOAL, Action.UPDATE);
        return ApiResponse.success(
                goalProgressUpdateService.addUpdate(
                        goalId,
                        dto,
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        hasGoalUpdatePermission
                ),
                "목표 진행률 업데이트 성공"
        );
    }

    @GetMapping("/{goalId}/updates")
    public ApiResponse<List<GoalProgressUpdateResDto>> listUpdates(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalProgressUpdateService.list(goalId, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표 진행률 업데이트 이력 조회 성공"
        );
    }
}
