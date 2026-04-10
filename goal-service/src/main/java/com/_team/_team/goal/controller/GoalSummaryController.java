package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.resdto.GoalOrgSummaryResDto;
import com._team._team.goal.service.GoalSummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 목표 집계 조회
@RestController
@RequestMapping("/goal")
public class GoalSummaryController {

    private final GoalSummaryService goalSummaryService;

    public GoalSummaryController(GoalSummaryService goalSummaryService) {
        this.goalSummaryService = goalSummaryService;
    }

    // 조직별 목표 집계 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/summary/by-org")
    public ApiResponse<List<GoalOrgSummaryResDto>> summaryByOrg(
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ApiResponse.success(
                goalSummaryService.summarizeByOrg(UUID.fromString(companyId), periodStart, periodEnd),
                "조직별 목표 집계 조회 성공");
    }
}
