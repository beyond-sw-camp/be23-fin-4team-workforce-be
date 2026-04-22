package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.GoalApprovalDecisionReqDto;
import com._team._team.goal.dto.reqdto.GoalApprovalRequestCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalApprovalWatchersReqDto;
import com._team._team.goal.dto.resdto.GoalApprovalResDto;
import com._team._team.goal.service.GoalApprovalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// 목표 승인 처리
@RestController
@RequestMapping("/goal")
public class GoalApprovalController {

    private final GoalApprovalService goalApprovalService;

    public GoalApprovalController(GoalApprovalService goalApprovalService) {
        this.goalApprovalService = goalApprovalService;
    }

    // 목표 승인 요청
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/approval/request")
    public ApiResponse<GoalApprovalResDto> request(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody GoalApprovalRequestCreateReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.request(goalId, UUID.fromString(memberId), UUID.fromString(companyId), dto),
                "목표 승인 요청 생성 성공");
    }

    // 목표 승인 조회 (가시성 또는 번들 요청자·승인자·워처)
    @GetMapping("/{goalId}/approval")
    public ApiResponse<GoalApprovalResDto> get(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalApprovalService.get(goalId, UUID.fromString(companyId), UUID.fromString(memberId)),
                "목표 승인 조회 성공");
    }

    // 목표 승인 처리
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/approval/{requestId}/approve")
    public ApiResponse<GoalApprovalResDto> approve(
            @PathVariable UUID goalId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody GoalApprovalDecisionReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.approve(goalId, requestId, UUID.fromString(memberId), UUID.fromString(companyId), dto),
                "목표 승인 처리 성공");
    }

    // 목표 반려 처리
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/approval/{requestId}/reject")
    public ApiResponse<GoalApprovalResDto> reject(
            @PathVariable UUID goalId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody GoalApprovalDecisionReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.reject(goalId, requestId, UUID.fromString(memberId), UUID.fromString(companyId), dto),
                "목표 반려 처리 성공");
    }

    // 승인 참조자 등록
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/approval/{requestId}/watchers")
    public ApiResponse<GoalApprovalResDto> addWatchers(
            @PathVariable UUID goalId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody GoalApprovalWatchersReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.addWatchers(goalId, requestId, UUID.fromString(companyId), dto),
                "승인 참조자 등록 성공");
    }
}
