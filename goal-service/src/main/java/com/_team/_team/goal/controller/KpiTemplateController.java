package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.KpiTemplateCreateReqDto;
import com._team._team.goal.dto.resdto.KpiTemplateResDto;
import com._team._team.goal.service.KpiTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// KPI 템플릿 생성, 목록 조회, 비활성화 컨트롤러
@RestController
@RequestMapping("/goal/kpi-template")
public class KpiTemplateController {

    final KpiTemplateService kpiTemplateService;

    @Autowired
    public KpiTemplateController(KpiTemplateService kpiTemplateService) {
        this.kpiTemplateService = kpiTemplateService;
    }

    // KPI 템플릿 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ApiResponse<KpiTemplateResDto> createKpiTemplate(
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid KpiTemplateCreateReqDto dto) {
        return ApiResponse.success(kpiTemplateService.createKpiTemplate(dto, UUID.fromString(companyId)), "KPI 템플릿이 생성되었습니다.");
    }

    // KPI 템플릿 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ApiResponse<List<KpiTemplateResDto>> listKpiTemplates(
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(kpiTemplateService.listKpiTemplates(UUID.fromString(companyId)), "KPI 템플릿 목록 조회 성공");
    }

    // KPI 템플릿 비활성화
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{kpiTemplateId}/deactivate")
    public ApiResponse<Void> deactivate(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID kpiTemplateId) {
        kpiTemplateService.deactivate(kpiTemplateId, UUID.fromString(companyId));
        return ApiResponse.success(null, "KPI 템플릿이 비활성화되었습니다.");
    }
}
