package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.EvaluationPolicyCreateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluationPolicyResDto;
import com._team._team.evaluation.service.EvaluationPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 평가 정책 생성, 목록 조회, 비활성화 컨트롤러
@RestController
@RequestMapping("/evaluation/policy")
public class EvaluationPolicyController {

    final EvaluationPolicyService evaluationPolicyService;

    @Autowired
    public EvaluationPolicyController(EvaluationPolicyService evaluationPolicyService) {
        this.evaluationPolicyService = evaluationPolicyService;
    }

    // 평가 정책 생성
    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<EvaluationPolicyResDto>> createPolicy(
            @Valid @RequestBody EvaluationPolicyCreateReqDto dto) {
        EvaluationPolicyResDto result = evaluationPolicyService.createPolicy(dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 정책이 생성되었습니다."));
    }

    // 평가 정책 목록 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluationPolicyResDto>>> listPolicies(
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<EvaluationPolicyResDto> result = activeOnly
                ? evaluationPolicyService.listActivePolicies(UUID.fromString(companyId))
                : evaluationPolicyService.listPolicies(UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 정책 목록 조회 성공"));
    }

    // 평가 정책 비활성화
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{policyId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivatePolicy(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID policyId) {
        evaluationPolicyService.deactivatePolicy(policyId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(null, "평가 정책이 비활성화되었습니다."));
    }
}
