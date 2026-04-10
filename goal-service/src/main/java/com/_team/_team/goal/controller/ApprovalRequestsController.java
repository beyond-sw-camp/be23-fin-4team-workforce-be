package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.GoalApprovalDecisionReqDto;
import com._team._team.goal.dto.resdto.GoalApprovalBundleDetailResDto;
import com._team._team.goal.dto.resdto.GoalApprovalBundleSummaryResDto;
import com._team._team.goal.service.GoalApprovalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 승인 대기함 조회
@RestController
@RequestMapping("/goal/approval-requests")
public class ApprovalRequestsController {

    private final GoalApprovalService goalApprovalService;

    public ApprovalRequestsController(GoalApprovalService goalApprovalService) {
        this.goalApprovalService = goalApprovalService;
    }

    // 승인 대기 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ApiResponse<List<GoalApprovalBundleSummaryResDto>> myPending(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalApprovalService.listPendingSummariesForApprover(
                        UUID.fromString(companyId), UUID.fromString(memberId)),
                "승인 대기 목록 조회 성공");
    }

    // 승인 요청 상세 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{requestId}")
    public ApiResponse<GoalApprovalBundleDetailResDto> detail(
            @PathVariable UUID requestId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalApprovalService.getBundleDetail(
                        requestId, UUID.fromString(companyId), UUID.fromString(memberId)),
                "승인 요청 상세 조회 성공");
    }

    // 승인 요청 흐름 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/history")
    public ApiResponse<List<GoalApprovalBundleSummaryResDto>> history(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalApprovalService.listHistorySummariesForMember(
                        UUID.fromString(companyId), UUID.fromString(memberId)),
                "승인 요청 흐름 조회 성공");
    }

    // 승인 요청 승인
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{requestId}/approve")
    public ApiResponse<GoalApprovalBundleDetailResDto> approve(
            @PathVariable UUID requestId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody(required = false) GoalApprovalDecisionReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.approveBundleOnly(
                        requestId, UUID.fromString(memberId), UUID.fromString(companyId), dto),
                "승인 처리되었습니다.");
    }

    // 승인 요청 반려
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{requestId}/reject")
    public ApiResponse<GoalApprovalBundleDetailResDto> reject(
            @PathVariable UUID requestId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody(required = false) GoalApprovalDecisionReqDto dto) {
        return ApiResponse.success(
                goalApprovalService.rejectBundleOnly(
                        requestId, UUID.fromString(memberId), UUID.fromString(companyId), dto),
                "반려 처리되었습니다.");
    }
}
