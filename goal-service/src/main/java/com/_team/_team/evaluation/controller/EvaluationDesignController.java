package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.DesignCreateReqDto;
import com._team._team.evaluation.dto.reqdto.DesignUpdateReqDto;
import com._team._team.evaluation.dto.resdto.DesignResDto;
import com._team._team.evaluation.service.EvaluationDesignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evaluation/evaluation-designs")
public class EvaluationDesignController {

    private final EvaluationDesignService designService;

    public EvaluationDesignController(EvaluationDesignService designService) {
        this.designService = designService;
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<DesignResDto>>> listDesigns(
            @RequestHeader("X-User-CompanyId") String companyId) {
        List<DesignResDto> result = designService.listDesigns(UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 설계 목록 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<DesignResDto>> createDesign(
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody DesignCreateReqDto dto) {
        DesignResDto result = designService.createDesign(UUID.fromString(companyId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 설계가 생성되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{designId}")
    public ResponseEntity<ApiResponse<DesignResDto>> getDesign(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID designId) {
        DesignResDto result = designService.getDesign(designId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 설계 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{designId}")
    public ResponseEntity<ApiResponse<DesignResDto>> updateDesign(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID designId,
            @RequestBody DesignUpdateReqDto dto) {
        DesignResDto result = designService.updateDesign(designId, UUID.fromString(companyId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 설계가 수정되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{designId}/preview")
    public ResponseEntity<ApiResponse<DesignResDto>> previewDesign(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID designId) {
        DesignResDto result = designService.getDesign(designId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 설계 미리보기"));
    }
}
