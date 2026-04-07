package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.EvaluationConfirmReqDto;
import com._team._team.evaluation.dto.reqdto.EvaluationCreateReqDto;
import com._team._team.evaluation.dto.reqdto.EvaluationScoreUpdateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluationResDto;
import com._team._team.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 평가 생성, 조회, 점수 수정, 제출, 확정 컨트롤러
@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    final EvaluationService evaluationService;

    @Autowired
    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    // 평가 생성
    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<EvaluationResDto>> createEvaluation(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody EvaluationCreateReqDto dto) {
        EvaluationResDto result = evaluationService.createEvaluation(UUID.fromString(memberId), UUID.fromString(companyId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 생성되었습니다."));
    }

    // 평가 상세 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{evaluationId}")
    public ResponseEntity<ApiResponse<EvaluationResDto>> getEvaluation(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID evaluationId) {
        EvaluationResDto result = evaluationService.getEvaluation(evaluationId, UUID.fromString(memberId), UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 조회 성공"));
    }

    // 평가 점수 수정
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/scores")
    public ResponseEntity<ApiResponse<EvaluationResDto>> updateScores(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID evaluationId,
            @RequestBody EvaluationScoreUpdateReqDto dto) {
        EvaluationResDto result = evaluationService.updateScores(evaluationId, UUID.fromString(memberId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 점수가 수정되었습니다."));
    }

    // 평가 제출
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/submit")
    public ResponseEntity<ApiResponse<EvaluationResDto>> submitEvaluation(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID evaluationId) {
        EvaluationResDto result = evaluationService.submitEvaluation(evaluationId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 제출되었습니다."));
    }

    // 평가 확정
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/confirm")
    public ResponseEntity<ApiResponse<EvaluationResDto>> confirmEvaluation(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID evaluationId,
            @RequestBody @Valid EvaluationConfirmReqDto dto) {
        EvaluationResDto result = evaluationService.confirmEvaluation(evaluationId, UUID.fromString(companyId), dto.getFinalScore(), dto.getGrade());
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 확정되었습니다."));
    }

    // 정책별 평가 목록 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<ApiResponse<List<EvaluationResDto>>> listByPolicy(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID policyId) {
        List<EvaluationResDto> result = evaluationService.listByPolicy(policyId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "정책별 평가 목록 조회 성공"));
    }

    // 평가자별 평가 목록 조회 (본인 것만 조회 가능하도록 수정)
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/evaluator/me")
    public ResponseEntity<ApiResponse<List<EvaluationResDto>>> listMyEvaluations(
            @RequestHeader("X-Member-Id") String memberId) {
        List<EvaluationResDto> result = evaluationService.listByEvaluator(UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "나의 평가 목록 조회 성공"));
    }

}
