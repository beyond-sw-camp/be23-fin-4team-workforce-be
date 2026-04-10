package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.PerformanceRecordCreateReqDto;
import com._team._team.goal.dto.reqdto.PerformanceRecordReviewReqDto;
import com._team._team.goal.dto.resdto.PerformanceRecordHistoryResDto;
import com._team._team.goal.dto.resdto.PerformanceRecordResDto;
import com._team._team.goal.service.PerformanceRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 목표별 실적 제출, 검토, 이력 조회 컨트롤러
@RestController
@RequestMapping("/goal/{goalId}/performance-record")
public class PerformanceRecordController {

    final PerformanceRecordService performanceRecordService;

    @Autowired
    public PerformanceRecordController(PerformanceRecordService performanceRecordService) {
        this.performanceRecordService = performanceRecordService;
    }

    // 실적 제출
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ApiResponse<PerformanceRecordResDto> submitRecord(
            @PathVariable UUID goalId,
            @RequestBody @Valid PerformanceRecordCreateReqDto dto,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                performanceRecordService.submitRecord(goalId, dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "실적이 제출되었습니다.");
    }

    // 실적 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ApiResponse<List<PerformanceRecordResDto>> listRecords(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(performanceRecordService.listRecords(goalId, UUID.fromString(companyId)), "실적 목록 조회 성공");
    }

    // 실적 검토
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{performanceRecordId}/review")
    public ApiResponse<PerformanceRecordResDto> reviewRecord(
            @PathVariable UUID goalId,
            @PathVariable UUID performanceRecordId,
            @RequestBody @Valid PerformanceRecordReviewReqDto dto,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                performanceRecordService.reviewRecord(goalId, performanceRecordId, dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "실적 검토가 완료되었습니다.");
    }

    // 실적 이력 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{performanceRecordId}/history")
    public ApiResponse<List<PerformanceRecordHistoryResDto>> getHistory(
            @PathVariable UUID goalId,
            @PathVariable UUID performanceRecordId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                performanceRecordService.getHistory(goalId, performanceRecordId, UUID.fromString(companyId)),
                "실적 이력 조회 성공");
    }
}