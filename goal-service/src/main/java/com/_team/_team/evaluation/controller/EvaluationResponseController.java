package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.CalibrationAdjustReqDto;
import com._team._team.evaluation.dto.reqdto.CalibrationBaselineReqDto;
import com._team._team.evaluation.dto.reqdto.ResponseSaveReqDto;
import com._team._team.evaluation.dto.resdto.GoalSummaryCardResDto;
import com._team._team.evaluation.dto.resdto.ResponseResDto;
import com._team._team.evaluation.service.EvaluationResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evaluation/evaluation-responses")
public class EvaluationResponseController {

    private final EvaluationResponseService responseService;
    private final com._team._team.evaluation.report.EvaluationReportService reportService;

    public EvaluationResponseController(EvaluationResponseService responseService,
                                         com._team._team.evaluation.report.EvaluationReportService reportService) {
        this.responseService = responseService;
        this.reportService = reportService;
    }

    // 내 평가 목록
    // + 평가자(evaluator_id == X-User-UUID) 본인 것만 반환하도록 서비스 레이어가 자체 필터.
    //  일반 사원이 평가자로 지정돼도 EVALUATION:READ 권한은 보통 없으므로 @CheckPermission 을
    //  걸면 403 으로 막혀 "할당된 평가가 없습니다" 로만 보이는 버그가 남.
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> listMyResponses(
            @RequestHeader("X-User-UUID") String memberId) {
        List<ResponseResDto> result = responseService.listMyResponses(UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "내 평가 목록 조회 성공"));
    }

    // 본인이 피평가자인 응답들 중 결과 공개된 시즌만 반환. 일반 사원도 접근 가능.
    // 2세그먼트 경로(/me/received)로 구성해 1세그먼트인 /{responseId} 매핑과 충돌하지 않도록 한다.
    @GetMapping("/me/received")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> listMyReceivedResults(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        List<ResponseResDto> result = responseService.listMyReceivedResults(
                UUID.fromString(memberId), UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "내 평가 결과 목록 조회 성공"));
    }

    // 특정 시즌에 대해 본인이 받은 평가 결과(결과 공개된 시즌만). 일반 사원 접근 가능.
    @GetMapping("/seasons/{seasonId}/my-result")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> listMySeasonResult(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        List<ResponseResDto> result = responseService.listMySeasonResult(
                seasonId, UUID.fromString(memberId), UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "내 평가 결과 조회 성공"));
    }

    // 응답 상세 — 본인(평가자)만 조회 가능. 서비스에서 evaluatorId == requesterId 검증.
    @GetMapping("/{responseId}")
    public ResponseEntity<ApiResponse<ResponseResDto>> getResponse(
            @RequestHeader("X-User-UUID") String memberId,
            @PathVariable UUID responseId) {
        ResponseResDto result = responseService.getResponse(responseId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 응답 조회 성공"));
    }

    // 임시 저장 — 서비스에서 evaluatorId 일치 검증.
    @PatchMapping("/{responseId}")
    public ResponseEntity<ApiResponse<ResponseResDto>> saveResponse(
            @RequestHeader("X-User-UUID") String memberId,
            @PathVariable UUID responseId,
            @RequestBody ResponseSaveReqDto dto) {
        ResponseResDto result = responseService.saveResponse(responseId, UUID.fromString(memberId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 임시 저장되었습니다."));
    }

    // 제출 — 서비스에서 evaluatorId 일치 검증.
    @PostMapping("/{responseId}/submit")
    public ResponseEntity<ApiResponse<ResponseResDto>> submitResponse(
            @RequestHeader("X-User-UUID") String memberId,
            @PathVariable UUID responseId) {
        ResponseResDto result = responseService.submitResponse(responseId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 제출되었습니다."));
    }

    // 재작성 허용 (관리자)
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{responseId}/reopen")
    public ResponseEntity<ApiResponse<ResponseResDto>> reopenResponse(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID responseId) {
        ResponseResDto result = responseService.reopenResponse(responseId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 재작성 가능 상태로 변경되었습니다."));
    }

    // ── 리마인드 ──

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/seasons/{seasonId}/reminders")
    public ResponseEntity<ApiResponse<Void>> sendBulkReminder(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        responseService.sendBulkReminder(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(null, "리마인드가 일괄 발송되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/seasons/{seasonId}/reminders/{memberId}")
    public ResponseEntity<ApiResponse<Void>> sendReminder(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @PathVariable UUID memberId) {
        responseService.sendReminder(seasonId, memberId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(null, "리마인드가 발송되었습니다."));
    }

    // ── 캘리브레이션 ──

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/seasons/{seasonId}/calibration")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> getCalibrationOverview(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        List<ResponseResDto> result = responseService.listBySeasonForProgress(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "캘리브레이션 현황 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/seasons/{seasonId}/calibration/baseline")
    public ResponseEntity<ApiResponse<Void>> applyBaseline(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestBody CalibrationBaselineReqDto dto) {
        responseService.applyBaseline(seasonId, UUID.fromString(companyId), dto.getRange(), dto.getBaselineValue());
        return ResponseEntity.ok(ApiResponse.success(null, "기준점이 적용되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/seasons/{seasonId}/calibration/adjustments")
    public ResponseEntity<ApiResponse<Void>> adjustCalibrations(
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestHeader(value = "X-User-UUID", required = false) String memberId,
            @PathVariable UUID seasonId,
            @RequestBody CalibrationAdjustReqDto dto) {
        UUID actorMemberId = (memberId != null && !memberId.isBlank()) ? UUID.fromString(memberId) : null;
        responseService.adjustCalibrations(seasonId, UUID.fromString(companyId), actorMemberId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "등급 조율이 저장되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/seasons/{seasonId}/calibration/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmCalibration(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        responseService.confirmCalibration(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(null, "캘리브레이션이 확정되었습니다."));
    }

    // ── 목표 요약 카드 (스냅샷 vs 현재) ──

    /**
     * 평가 응답에 포함된 타깃 목표 스냅샷과 현재 값을 비교하여 요약 카드 목록을 반환한다.
     * 평가 작성 화면(EvaluationWritePage)에서 평가자가 피평가자의 목표 현황을 확인할 때 사용한다.
     * 서비스에서 evaluatorId 일치 검증 — 별도 권한 체크 불요.
     */
    @GetMapping("/{responseId}/goal-summaries")
    public ResponseEntity<ApiResponse<List<GoalSummaryCardResDto>>> getGoalSummaries(
            @RequestHeader("X-User-UUID") String memberId,
            @PathVariable UUID responseId) {
        List<GoalSummaryCardResDto> result =
                responseService.listGoalSummaries(responseId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "목표 요약 카드 조회 성공"));
    }

    /**
     * 본인(피평가자) 기준, 특정 시즌의 목표 요약 카드(스냅샷 vs 현재).
     * 자기평가(SELF) 응답이 있으면 그 스냅샷을 사용하고, 없거나 비어 있으면 시즌 기간과 겹치는 본인 목표로 폴백한다.
     * 일반 사원도 접근 가능하도록 별도 권한 어노테이션 없음 — 서비스에서 회사·본인 검증.
     */
    @GetMapping("/seasons/{seasonId}/my-goal-summaries")
    public ResponseEntity<ApiResponse<List<GoalSummaryCardResDto>>> listMyGoalSummariesForSeason(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestParam(value = "completedOnly", defaultValue = "false") boolean completedOnly) {
        List<GoalSummaryCardResDto> result = responseService.listMyGoalSummariesForSeason(
                seasonId,
                UUID.fromString(memberId),
                UUID.fromString(companyId),
                completedOnly);
        return ResponseEntity.ok(ApiResponse.success(result, "내 목표 요약 카드 조회 성공"));
    }

    // ── 리포트 다운로드 ──

    /**
     * 본인이 받은 특정 시즌의 평가 결과를 PDF 로 다운로드.
     * 일반 사원도 접근 가능 — 서비스에서 본인 / 시즌 공개 여부 검증.
     */
    @GetMapping("/seasons/{seasonId}/my-report")
    public org.springframework.http.ResponseEntity<byte[]> downloadMyReport(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        byte[] pdf = reportService.renderMySeasonReport(
                seasonId, UUID.fromString(memberId), UUID.fromString(companyId));
        String filename = "evaluation-report-" + seasonId + ".pdf";
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    // ── 진행도 / 분석 ──

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/seasons/{seasonId}/progress")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> getProgress(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false) String q) {
        List<ResponseResDto> result = responseService.listBySeasonForProgress(
                seasonId, UUID.fromString(companyId), q);
        return ResponseEntity.ok(ApiResponse.success(result, "진행도 조회 성공"));
    }

}
