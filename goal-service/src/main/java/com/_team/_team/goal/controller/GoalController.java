package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalAggregateResDto;
import com._team._team.goal.dto.resdto.GoalKrResDto;
import com._team._team.goal.dto.resdto.GoalObjectiveResDto;
import com._team._team.goal.dto.resdto.GoalViewResDto;
import com._team._team.goal.permission.PositionPermissionReader;
import com._team._team.goal.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/goal")
public class GoalController {

    private final GoalService goalService;
    private final PositionPermissionReader positionPermissionReader;

    public GoalController(GoalService goalService,
                          PositionPermissionReader positionPermissionReader) {
        this.goalService = goalService;
        this.positionPermissionReader = positionPermissionReader;
    }

    @GetMapping("/me")
    public ApiResponse<List<GoalKrResDto>> myGoals(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) GoalStatus status) {
        return ApiResponse.success(
                goalService.listMyGoals(UUID.fromString(memberId), UUID.fromString(companyId), status),
                "My goals loaded successfully."
        );
    }

    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/company")
    public ApiResponse<List<GoalViewResDto>> companyGoals(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) KpiCycle cycle,
            HttpServletRequest request) {
        boolean hasEvalRead = positionPermissionReader.hasPermission(request, Resource.EVALUATION, Action.READ);
        return ApiResponse.success(
                goalService.listCompanyGoals(
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        cycle,
                        hasEvalRead
                ),
                "Company goals loaded successfully."
        );
    }

    @GetMapping("/organization")
    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.READ)
    public ApiResponse<List<GoalKrResDto>> orgGoals(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam UUID orgId) {
        return ApiResponse.success(
                goalService.listOrgGoals(UUID.fromString(memberId), orgId, UUID.fromString(companyId)),
                "Organization member KRs loaded successfully."
        );
    }

    @GetMapping("/objectives/me")
    public ApiResponse<List<GoalObjectiveResDto>> myObjectives(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) KpiCycle cycle) {
        return ApiResponse.success(
                goalService.listMyObjectives(UUID.fromString(memberId), UUID.fromString(companyId), cycle),
                "Managed organization objectives loaded successfully."
        );
    }

    @GetMapping("/objectives/available")
    public ApiResponse<List<GoalObjectiveResDto>> availableObjectives(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) KpiCycle cycle) {
        return ApiResponse.success(
                goalService.listAvailableObjectives(UUID.fromString(memberId), UUID.fromString(companyId), cycle),
                "Available objectives loaded successfully."
        );
    }

    @GetMapping("/objectives/organization")
    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.READ)
    public ApiResponse<List<GoalObjectiveResDto>> organizationObjectives(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam UUID orgId,
            @RequestParam(required = false) KpiCycle cycle) {
        return ApiResponse.success(
                goalService.listOrgObjectives(UUID.fromString(memberId), orgId, UUID.fromString(companyId), cycle),
                "Organization objectives loaded successfully."
        );
    }

    @PostMapping
    public ApiResponse<GoalViewResDto> create(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid GoalCreateReqDto dto,
            HttpServletRequest request) {
        if (dto.getOwnerType() == com._team._team.goal.domain.enums.GoalOwnerType.ORGANIZATION
                && !positionPermissionReader.canCreateOrganizationScopedGoal(request)) {
            throw new com._team._team.dto.BusinessException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "You do not have permission to create organization objectives."
            );
        }
        return ApiResponse.success(
                goalService.create(dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "Goal created successfully."
        );
    }

    @GetMapping("/{goalId}")
    public ApiResponse<GoalViewResDto> get(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasEvalRead = positionPermissionReader.hasPermission(request, Resource.EVALUATION, Action.READ);
        return ApiResponse.success(
                goalService.get(goalId, UUID.fromString(memberId), UUID.fromString(companyId), hasEvalRead),
                "Goal loaded successfully."
        );
    }

    @PatchMapping("/{goalId}")
    public ApiResponse<GoalViewResDto> update(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid GoalUpdateReqDto dto,
            HttpServletRequest request) {
        boolean canManageOrgGoal = positionPermissionReader.canCreateOrganizationScopedGoal(request);
        return ApiResponse.success(
                goalService.update(goalId, dto, UUID.fromString(memberId), UUID.fromString(companyId), canManageOrgGoal),
                "Goal updated successfully."
        );
    }

    @GetMapping("/{goalId}/aggregate")
    public ApiResponse<GoalAggregateResDto> aggregate(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasEvalRead = positionPermissionReader.hasPermission(request, Resource.EVALUATION, Action.READ);
        return ApiResponse.success(
                goalService.getAggregate(goalId, UUID.fromString(memberId), UUID.fromString(companyId), hasEvalRead),
                "Objective aggregate loaded successfully."
        );
    }

    @GetMapping("/{goalId}/children")
    public ApiResponse<List<GoalKrResDto>> children(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasEvalRead = positionPermissionReader.hasPermission(request, Resource.EVALUATION, Action.READ);
        return ApiResponse.success(
                goalService.listObjectiveChildren(goalId, UUID.fromString(memberId), UUID.fromString(companyId), hasEvalRead),
                "Objective child KRs loaded successfully."
        );
    }

    @DeleteMapping("/{goalId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean canManageOrgGoal = positionPermissionReader.canCreateOrganizationScopedGoal(request);
        goalService.delete(goalId, UUID.fromString(memberId), UUID.fromString(companyId), canManageOrgGoal);
        return ApiResponse.success(null, "Goal deleted successfully.");
    }

    @PostMapping("/{goalId}/cancel")
    public ApiResponse<GoalViewResDto> cancel(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean canManageOrgGoal = positionPermissionReader.canCreateOrganizationScopedGoal(request);
        return ApiResponse.success(
                goalService.cancelMine(goalId, UUID.fromString(memberId), UUID.fromString(companyId), canManageOrgGoal),
                "Goal cancelled successfully."
        );
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{goalId}/skip")
    public ApiResponse<GoalViewResDto> forceSkip(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.success(
                goalService.forceSkipByHr(goalId, reason, UUID.fromString(companyId)),
                "Goal skipped by HR successfully."
        );
    }
}
