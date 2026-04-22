package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.KpiTemplateCreateReqDto;
import com._team._team.goal.dto.reqdto.KpiTemplateGenerateReqDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.dto.resdto.KpiTemplateResDto;
import com._team._team.goal.service.KpiTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 템플릿 조회/생성
@RestController
@RequestMapping("/goal/kpi-template")
public class KpiTemplateController {

    final KpiTemplateService kpiTemplateService;

    @Autowired
    public KpiTemplateController(KpiTemplateService kpiTemplateService) {
        this.kpiTemplateService = kpiTemplateService;
    }

    // 템플릿 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ApiResponse<KpiTemplateResDto> createKpiTemplate(
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid KpiTemplateCreateReqDto dto) {
        return ApiResponse.success(
                kpiTemplateService.createKpiTemplate(dto, UUID.fromString(companyId)), "KPI 템플릿이 생성되었습니다.");
    }

    // 템플릿 목록 조회 (회사 스코프는 서비스에서 검증)
    @GetMapping
    public ApiResponse<List<KpiTemplateResDto>> listKpiTemplates(
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                kpiTemplateService.listKpiTemplates(UUID.fromString(companyId)), "KPI 템플릿 목록 조회 성공");
    }

    // 템플릿 상세 조회
    @GetMapping("/{kpiTemplateId}")
    public ApiResponse<KpiTemplateResDto> getKpiTemplate(
            @PathVariable UUID kpiTemplateId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                kpiTemplateService.getKpiTemplate(kpiTemplateId, UUID.fromString(companyId)), "KPI 템플릿 상세 조회 성공");
    }

    // 템플릿 비활성화
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{kpiTemplateId}/deactivate")
    public ApiResponse<Void> deactivate(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID kpiTemplateId) {
        kpiTemplateService.deactivate(kpiTemplateId, UUID.fromString(companyId));
        return ApiResponse.success(null, "KPI 템플릿이 비활성화되었습니다.");
    }

    // 템플릿 기반 목표 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping("/{kpiTemplateId}/generate")
    public ApiResponse<List<GoalResDto>> generate(
            @PathVariable UUID kpiTemplateId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid KpiTemplateGenerateReqDto dto) {
        return ApiResponse.success(
                kpiTemplateService.generateGoals(
                        kpiTemplateId, dto, UUID.fromString(companyId), UUID.fromString(memberId)),
                "템플릿 기반 목표가 생성되었습니다.");
    }
}
