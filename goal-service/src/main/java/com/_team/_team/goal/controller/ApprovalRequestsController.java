package com._team._team.goal.controller;

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
    // NOTE: 지정된 승인자(approverId == X-User-UUID) 본인의 건만 반환하도록
    //       서비스 레이어에서 필터하므로 별도의 GOAL:READ 권한은 요구하지 않는다.
    //       (@CheckPermission 을 걸면 일반 사원이 승인자로 지정됐을 때 403 으로 막힘)
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
    // 서비스 레이어에서 isReaderInvolved(요청자/승인자/워처)로 접근 통제 — 권한 체크 불요.
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

    // 내가 보낸 요청(이력) — requestedBy == X-User-UUID 로 자체 필터.
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
    // 서비스 레이어에서 bundle.approverId == approverId 를 검증 — 권한 체크 불요.
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

    // 승인 요청 반려 — 승인자 본인만 가능한지 서비스 레이어에서 검증.
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
