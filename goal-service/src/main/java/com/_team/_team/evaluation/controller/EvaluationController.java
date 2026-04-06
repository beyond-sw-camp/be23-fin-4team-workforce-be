package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.EvaluationCreateReqDto;
import com._team._team.evaluation.dto.reqdto.EvaluationScoreUpdateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluationResDto;
import com._team._team.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
            @Valid @RequestBody EvaluationCreateReqDto dto) {
        EvaluationResDto result = evaluationService.createEvaluation(dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 생성되었습니다."));
    }

    // 평가 상세 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{evaluationId}")
    public ResponseEntity<ApiResponse<EvaluationResDto>> getEvaluation(
            @PathVariable UUID evaluationId) {
        EvaluationResDto result = evaluationService.getEvaluation(evaluationId);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 조회 성공"));
    }

    // 평가 점수 수정
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/scores")
    public ResponseEntity<ApiResponse<EvaluationResDto>> updateScores(
            @PathVariable UUID evaluationId,
            @RequestBody EvaluationScoreUpdateReqDto dto) {
        EvaluationResDto result = evaluationService.updateScores(evaluationId, dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 점수가 수정되었습니다."));
    }

    // 평가 제출
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/submit")
    public ResponseEntity<ApiResponse<EvaluationResDto>> submitEvaluation(
            @PathVariable UUID evaluationId) {
        EvaluationResDto result = evaluationService.submitEvaluation(evaluationId);
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 제출되었습니다."));
    }

    // 평가 확정
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{evaluationId}/confirm")
    public ResponseEntity<ApiResponse<EvaluationResDto>> confirmEvaluation(
            @PathVariable UUID evaluationId,
            @RequestBody ConfirmRequest request) {
        EvaluationResDto result = evaluationService.confirmEvaluation(evaluationId, request.getFinalScore(), request.getGrade());
        return ResponseEntity.ok(ApiResponse.success(result, "평가가 확정되었습니다."));
    }

    // 정책별 평가 목록 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<ApiResponse<List<EvaluationResDto>>> listByPolicy(
            @PathVariable UUID policyId) {
        List<EvaluationResDto> result = evaluationService.listByPolicy(policyId);
        return ResponseEntity.ok(ApiResponse.success(result, "정책별 평가 목록 조회 성공"));
    }

    // 평가자별 평가 목록 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/evaluator/{evaluatorId}")
    public ResponseEntity<ApiResponse<List<EvaluationResDto>>> listByEvaluator(
            @PathVariable UUID evaluatorId) {
        List<EvaluationResDto> result = evaluationService.listByEvaluator(evaluatorId);
        return ResponseEntity.ok(ApiResponse.success(result, "평가자별 평가 목록 조회 성공"));
    }

    @Getter
    @NoArgsConstructor
    public static class ConfirmRequest {
        private BigDecimal finalScore;
        private String grade;
    }
}
