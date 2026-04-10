package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.CalibrationAdjustReqDto;
import com._team._team.evaluation.dto.reqdto.CalibrationBaselineReqDto;
import com._team._team.evaluation.dto.reqdto.ResponseSaveReqDto;
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

    public EvaluationResponseController(EvaluationResponseService responseService) {
        this.responseService = responseService;
    }

    // 내 평가 목록
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> listMyResponses(
            @RequestHeader("X-Member-Id") String memberId) {
        List<ResponseResDto> result = responseService.listMyResponses(UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "내 평가 목록 조회 성공"));
    }

    // 응답 상세
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{responseId}")
    public ResponseEntity<ApiResponse<ResponseResDto>> getResponse(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID responseId) {
        ResponseResDto result = responseService.getResponse(responseId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 응답 조회 성공"));
    }

    // 임시 저장
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{responseId}")
    public ResponseEntity<ApiResponse<ResponseResDto>> saveResponse(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID responseId,
            @RequestBody ResponseSaveReqDto dto) {
        ResponseResDto result = responseService.saveResponse(responseId, UUID.fromString(memberId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 임시 저장되었습니다."));
    }

    // 제출
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{responseId}/submit")
    public ResponseEntity<ApiResponse<ResponseResDto>> submitResponse(
            @RequestHeader("X-Member-Id") String memberId,
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
        // TODO: 기준점 기반 점수 재계산 로직
        return ResponseEntity.ok(ApiResponse.success(null, "기준점이 적용되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/seasons/{seasonId}/calibration/adjustments")
    public ResponseEntity<ApiResponse<Void>> adjustCalibrations(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestBody CalibrationAdjustReqDto dto) {
        responseService.adjustCalibrations(seasonId, UUID.fromString(companyId), dto);
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

    // ── 진행도 / 분석 / 오류감지 ──

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/seasons/{seasonId}/progress")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> getProgress(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        List<ResponseResDto> result = responseService.listBySeasonForProgress(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "진행도 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/seasons/{seasonId}/anomalies")
    public ResponseEntity<ApiResponse<List<ResponseResDto>>> listAnomalies(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        List<ResponseResDto> result = responseService.listAnomalies(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "오류 감지 목록 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/seasons/{seasonId}/anomalies/{responseId}/request-review")
    public ResponseEntity<ApiResponse<ResponseResDto>> requestReview(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @PathVariable UUID responseId) {
        ResponseResDto result = responseService.reopenResponse(responseId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "재검토가 요청되었습니다."));
    }
}
