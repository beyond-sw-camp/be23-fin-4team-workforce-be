package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 목표(Goal) CRUD 및 상태 관리 컨트롤러
@RestController
@RequestMapping("/goal")
public class GoalController {

    final GoalService goalService;

    @Autowired
    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    // 목표 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ApiResponse<GoalResDto> createGoal(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid GoalCreateReqDto dto) {
        return ApiResponse.success(goalService.createGoal(dto, UUID.fromString(memberId), UUID.fromString(companyId)), "목표가 생성되었습니다.");
    }

    // 목표 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ApiResponse<List<GoalResDto>> listGoals(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.listGoals(UUID.fromString(companyId), UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표 목록 조회 성공");
    }

    // 목표 상세 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{goalId}")
    public ApiResponse<GoalResDto> getGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.getGoal(goalId, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표 조회 성공");
    }

    // 목표 수정
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}")
    public ApiResponse<GoalResDto> updateGoal(
            @PathVariable UUID goalId,
            @RequestBody @Valid GoalUpdateReqDto dto,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.updateGoal(goalId, dto, UUID.fromString(companyId)),
                "목표가 수정되었습니다.");
    }

    // 목표 활성화
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/activate")
    public ApiResponse<GoalResDto> activateGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.activateGoal(goalId, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표가 활성화되었습니다.");
    }

    // 목표 완료
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/complete")
    public ApiResponse<GoalResDto> completeGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.completeGoal(goalId, UUID.fromString(companyId)),
                "목표가 완료되었습니다.");
    }

    // 목표 취소
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/cancel")
    public ApiResponse<GoalResDto> cancelGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.cancelGoal(goalId, UUID.fromString(companyId)),
                "목표가 취소되었습니다.");
    }
}