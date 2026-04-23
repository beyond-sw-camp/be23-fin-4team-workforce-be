package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.enums.*;
import com._team._team.goal.dto.reqdto.GoalApprovalRequestCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalCompletionSubmitReqDto;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.repository.GoalRepository;
import com._team._team.goal.repository.KpiTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoalService {

    final GoalRepository goalRepository;
    final KpiTemplateRepository kpiTemplateRepository;
    final GoalRollupService goalRollupService;
    final GoalTreeService goalTreeService;
    final GoalActivityService goalActivityService;
    final GoalApprovalService goalApprovalService;

    @Autowired
    public GoalService(GoalRepository goalRepository,
                       KpiTemplateRepository kpiTemplateRepository,
                       GoalRollupService goalRollupService,
                       GoalTreeService goalTreeService,
                       GoalActivityService goalActivityService,
                       GoalApprovalService goalApprovalService) {
        this.goalRepository = goalRepository;
        this.kpiTemplateRepository = kpiTemplateRepository;
        this.goalRollupService = goalRollupService;
        this.goalTreeService = goalTreeService;
        this.goalActivityService = goalActivityService;
        this.goalApprovalService = goalApprovalService;
    }

    @Transactional
    public GoalResDto createGoal(GoalCreateReqDto dto, UUID requesterId, UUID requesterCompanyId) {
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }
        if (!dto.getCompanyId().equals(requesterCompanyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 목표를 생성할 수 없습니다.");
        }

        // [TEAM 목표] 책임자는 팀 목표에서 필수, MEMBER 목표에서는 무시.
        UUID responsibleMemberId = resolveResponsibleMemberIdForCreate(dto);

        BigDecimal baseline = dto.getBaseline() != null ? dto.getBaseline() : BigDecimal.ZERO;
        if (dto.getTargetValue().compareTo(baseline) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "목표값은 기준값과 달라야 합니다.");
        }

        KpiTemplate kpiTemplate = null;
        if (dto.getKpiTemplateId() != null) {
            kpiTemplate = kpiTemplateRepository.findById(dto.getKpiTemplateId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "KPI 템플릿을 찾을 수 없습니다. id=" + dto.getKpiTemplateId()));
            if (kpiTemplate.getCompanyId() != null && !kpiTemplate.getCompanyId().equals(requesterCompanyId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 KPI 템플릿을 사용할 수 없습니다.");
            }
            if (!kpiTemplate.isActive()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "비활성 KPI 템플릿으로 목표를 생성할 수 없습니다. id=" + dto.getKpiTemplateId());
            }
        }

        MeasureType measureType = dto.getMeasureType() != null ? dto.getMeasureType() : MeasureType.HIGHER_BETTER;
        UnitType unitType = dto.getUnitType() != null ? dto.getUnitType() : UnitType.NUMBER;
        int capPct = dto.getCapPct() != null ? dto.getCapPct() : (kpiTemplate != null ? kpiTemplate.getCapPct() : 100);
        if (capPct < 1) {
            capPct = 100;
        }

        String unitLabel = dto.getUnitLabel();
        if (unitLabel == null || unitLabel.isBlank()) {
            unitLabel = kpiTemplate != null ? kpiTemplate.getUnitLabel() : "건";
        }

        KpiCycle cycle = dto.getCycle() != null ? dto.getCycle() : (kpiTemplate != null ? kpiTemplate.getCycle() : KpiCycle.ANYTIME);

        Goal parentGoal = null;
        if (dto.getParentGoalId() != null) {
            parentGoal = goalRepository.findById(dto.getParentGoalId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 목표를 찾을 수 없습니다. id=" + dto.getParentGoalId()));
            if (!parentGoal.getCompanyId().equals(requesterCompanyId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 목표를 상위 목표로 지정할 수 없습니다.");
            }
            if (parentGoal.getStatus() == GoalStatus.CANCELLED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "취소된 목표는 상위 목표로 지정할 수 없습니다.");
            }
        }

        boolean auto = dto.getAutoUpdate() == null || dto.getAutoUpdate();
        RollupPolicy rollup = dto.getRollupPolicy() != null ? dto.getRollupPolicy() : RollupPolicy.CHILDREN_AVG;

        String desc = dto.getDescription() != null ? dto.getDescription() : "";
        GoalKind kind = dto.getGoalKind() != null ? dto.getGoalKind() : GoalKind.KR;

        Goal goal = Goal.builder()
                .kpiTemplate(kpiTemplate)
                .cycle(cycle)
                .companyId(requesterCompanyId)
                .parentGoal(parentGoal)
                .goalKind(kind)
                .autoUpdate(auto)
                .healthStatus(GoalHealthStatus.NOT_STARTED)
                .visibleTeamIds(dto.getVisibleTeamIds() != null ? new ArrayList<>(dto.getVisibleTeamIds()) : new ArrayList<>())
                .participantMemberIds(dto.getMemberIds() != null ? new ArrayList<>(dto.getMemberIds()) : new ArrayList<>())
                .ownerType(dto.getOwnerType())
                .ownerId(dto.getOwnerId())
                .title(dto.getTitle())
                .description(desc)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .measureType(measureType)
                .unitType(unitType)
                .unitLabel(unitLabel)
                .baseline(baseline)
                .targetValue(dto.getTargetValue())
                .capPct(capPct)
                .contributionPct(dto.getContributionPct())
                .weightPct(dto.getWeightPct())
                .visibility(dto.getVisibility())
                .status(GoalStatus.DRAFT)
                .rollupPolicy(rollup)
                // 생성 시 지정한 approverId 를 "종료 승인자" 로 영구 저장.
                // 완료 제출 때 별도 지정 없이도 이 값이 기본 승인자로 사용됨.
                .completionApproverId(dto.getApproverId())
                // [TEAM 목표] 책임자 — MEMBER 목표는 null.
                .responsibleMemberId(responsibleMemberId)
                .build();

        Goal saved = goalRepository.save(goal);
        goalRollupService.recalculateFrom(saved.getGoalId());
        goalActivityService.append(saved.getGoalId(), GoalActivityType.GOAL_CREATED, requesterId, "목표 생성", null);

        // [D-2] 승인/활성 분기 단일화:
        // - 프론트 호환: requireApproval/approverId 필드가 들어오면 여기서 번들 생성 (기존 직접 호출 경로)
        // - KpiTemplateService.generateGoals 경로: requireApproval=false + activateImmediately=false 로 호출 후
        //   호출부에서 tplPolicy.requiresActivation() 체크 → createBundleForGoals 명시 호출.
        // 두 경로는 requireApproval 플래그 값으로 완전히 배타적. 중복 승인 번들 생성 위험 없음.
        // 향후 리팩터: requireApproval 플래그 deprecate 하고 호출부 명시 방식으로 단일화.
        boolean needApproval = Boolean.TRUE.equals(dto.getRequireApproval())
                && dto.getApproverId() != null;
        boolean activateNow = dto.getActivateImmediately() == null || dto.getActivateImmediately();
        if (needApproval && activateNow) {
            GoalApprovalRequestCreateReqDto ar = new GoalApprovalRequestCreateReqDto();
            ar.setApproverId(dto.getApproverId());
            goalApprovalService.createBundleForGoals(List.of(saved.getGoalId()), requesterId, requesterCompanyId, ar);
        } else if (!needApproval && activateNow) {
            saved.activate(requesterId);
        }

        goalRollupService.recalculateFrom(saved.getGoalId());
        return GoalResDto.from(goalRepository.findById(saved.getGoalId()).orElse(saved));
    }

    @Transactional(readOnly = true)
    public List<GoalResDto> listGoals(UUID companyId, UUID requesterId, UUID requesterCompanyId,
                                       UUID parentId, KpiCycle cycle, UUID ownerFilter, GoalHealthStatus status,
                                       Integer depth) {
        validateSameCompany(companyId, requesterCompanyId);
        List<Goal> allGoals = goalRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);

        // [P-1] 회사의 모든 goal 을 한 번에 조회해서 parentId → Goal 맵을 메모리에 구성.
        // depth 필터 적용 시 기존처럼 findById 를 목표·깊이마다 호출하지 않고 맵 조회만 수행.
        java.util.Map<UUID, Goal> byId = new java.util.HashMap<>();
        for (Goal g : allGoals) byId.put(g.getGoalId(), g);

        List<Goal> goals = allGoals.stream()
                .filter(g -> isVisible(g, requesterId))
                .filter(g -> parentId == null || (g.getParentGoal() != null && g.getParentGoal().getGoalId().equals(parentId)))
                .filter(g -> cycle == null || g.getCycle() == cycle)
                .filter(g -> ownerFilter == null || g.getOwnerId().equals(ownerFilter))
                .filter(g -> status == null || g.getHealthStatus() == status)
                .filter(g -> depth == null || computeDepthInMemory(g, byId) == depth)
                .collect(Collectors.toList());
        return goalTreeService.decorate(goals);
    }

    /**
     * [P-1] 메모리 맵 기반 depth 계산. 기존의 findById N+1 제거.
     * 사이클(부모 참조 순환) 이 생겨도 방문 표시로 무한 루프 방지.
     */
    private int computeDepthInMemory(Goal g, java.util.Map<UUID, Goal> byId) {
        int d = 0;
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        Goal c = g;
        while (c != null && c.getParentGoal() != null) {
            UUID parentId = c.getParentGoal().getGoalId();
            if (!visited.add(parentId)) break; // 사이클 방어
            d++;
            c = byId.get(parentId);
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<GoalResDto> listGoals(UUID companyId, UUID requesterId, UUID requesterCompanyId) {
        return listGoals(companyId, requesterId, requesterCompanyId, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<GoalResDto> treeGoals(UUID companyId, UUID requesterId, UUID requesterCompanyId, String scope, UUID ownerId, LocalDate periodStart, LocalDate periodEnd) {
        validateSameCompany(companyId, requesterCompanyId);
        List<Goal> base;
        if ("mine".equalsIgnoreCase(scope)) {
            base = goalRepository.findByCompanyIdAndOwnerId(companyId, requesterId);
        } else if (ownerId != null) {
            base = goalRepository.findByCompanyIdAndOwnerId(companyId, ownerId);
        } else {
            base = goalRepository.findByCompanyId(companyId);
        }
        List<Goal> visible = base.stream()
                .filter(g -> isVisible(g, requesterId))
                .filter(g -> periodStart == null || !g.getStartDate().isBefore(periodStart))
                .filter(g -> periodEnd == null || !g.getEndDate().isAfter(periodEnd))
                .toList();
        return goalTreeService.decorate(visible);
    }

    /**
     * 동일 회사이며 목표 가시성 규칙을 만족할 때만 Goal 엔티티를 반환합니다.
     * 댓글·활동·진행 이력 등 하위 리소스 조회 시 공통으로 사용합니다.
     */
    @Transactional(readOnly = true)
    public Goal requireReadableGoal(UUID goalId, UUID requesterId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        validateSameCompany(goal.getCompanyId(), requesterCompanyId);
        if (!isVisible(goal, requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "해당 목표에 대한 접근 권한이 없습니다.");
        }
        return goal;
    }

    /** 승인자·요청자·워처 등 가시성 외 규칙에서 사용 */
    public boolean isVisibleToMember(Goal goal, UUID memberId) {
        return isVisible(goal, memberId);
    }

    @Transactional(readOnly = true)
    public GoalResDto getGoal(UUID goalId, UUID requesterId, UUID requesterCompanyId) {
        return GoalResDto.from(requireReadableGoal(goalId, requesterId, requesterCompanyId));
    }

    @Transactional
    public GoalResDto updateGoal(
            UUID goalId,
            GoalUpdateReqDto dto,
            UUID requesterCompanyId,
            UUID actorMemberId,
            boolean hasGoalUpdatePermission) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "완료되거나 취소된 목표는 수정할 수 없습니다. 현재 상태: " + goal.getStatus());
        }
        // 결재 진행 중에는 목표를 수정할 수 없도록 차단한다.
        // (승인자가 검토하는 동안 내용이 바뀌면 결재 근거가 흐트러짐)
        if (goal.getGoalApprovalStatus() == GoalApprovalStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결재 진행 중인 목표는 수정할 수 없습니다. 결재 완료 또는 반려 후 수정해 주세요.");
        }
        boolean isOwnMemberGoal =
                goal.getOwnerType() == GoalOwnerType.MEMBER
                        && actorMemberId != null
                        && goal.getOwnerId().equals(actorMemberId);
        // TEAM 목표는 책임자도 자기 목표 편집으로 간주 (운영 우회 분기와 별개)
        boolean isTeamResponsible =
                goal.getOwnerType() == GoalOwnerType.ORGANIZATION
                        && actorMemberId != null
                        && actorMemberId.equals(goal.getResponsibleMemberId());
        if (!hasGoalUpdatePermission && !isOwnMemberGoal && !isTeamResponsible) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "본인 목표(또는 팀 목표 책임자) 및 목표 수정 권한자만 수정할 수 있습니다.");
        }

        goal.updateExtended(
                dto.getTitle(),
                dto.getDescription(),
                dto.getVisibility(),
                dto.getWeightPct(),
                dto.getContributionPct(),
                dto.getGoalKind(),
                dto.getAutoUpdate(),
                dto.getHealthStatus(),
                dto.getVisibleTeamIds(),
                dto.getMemberIds(),
                dto.getCycle());

        // [TEAM 목표] 책임자 교체 — GOAL:UPDATE 권한자만 변경 가능 (셀프 교체 차단).
        if (dto.getResponsibleMemberId() != null) {
            if (goal.getOwnerType() != GoalOwnerType.ORGANIZATION) {
                // MEMBER 목표에 책임자 지정이 오면 무시 (엔티티 저장 안 함)
            } else if (!hasGoalUpdatePermission) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "팀 목표 책임자 교체는 목표 수정 권한자만 가능합니다.");
            } else {
                goal.updateResponsibleMemberId(dto.getResponsibleMemberId());
            }
        }

        if (dto.getRollupPolicy() != null) {
            goal.updateRollupPolicy(dto.getRollupPolicy());
        }
        if (dto.getParentGoalId() != null) {
            // 자기 자신 혹은 자손 목표로의 부모 지정은 트리에 사이클을 만들어버리므로 차단.
            if (dto.getParentGoalId().equals(goal.getGoalId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "목표 자신을 상위 목표로 지정할 수 없습니다.");
            }
            if (isDescendant(goal.getGoalId(), dto.getParentGoalId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "하위 목표를 상위 목표로 지정할 수 없습니다 (사이클 방지).");
            }
            Goal parent = goalRepository.findById(dto.getParentGoalId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 목표를 찾을 수 없습니다. id=" + dto.getParentGoalId()));
            validateSameCompany(parent.getCompanyId(), requesterCompanyId);
            if (parent.getStatus() == GoalStatus.CANCELLED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "취소된 목표는 상위 목표로 지정할 수 없습니다.");
            }
            goal.updateParentGoal(parent);
        }
        goalRollupService.recalculateFrom(goal.getGoalId());
        goalActivityService.append(goal.getGoalId(), GoalActivityType.GOAL_METADATA_UPDATED,
                actorMemberId != null ? actorMemberId : requesterCompanyId, "목표 메타 수정", null);
        return GoalResDto.from(goal);
    }

    @Transactional
    public void deleteGoal(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표를 찾을 수 없습니다."));
        validateSameCompany(goal.getCompanyId(), requesterCompanyId);
        if (goalRepository.countByParentGoal_GoalId(goalId) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "하위 목표가 있어 삭제할 수 없습니다.");
        }
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalResDto activateGoal(UUID goalId, UUID approvedBy, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() != GoalStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 상태의 목표만 활성화할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        goal.activate(approvedBy);
        goalRollupService.recalculateFrom(goal.getGoalId());
        goalActivityService.append(goal.getGoalId(), GoalActivityType.APPROVED, approvedBy, "목표 활성화", null);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto completeGoal(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "ACTIVE 상태의 목표만 완료할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        // [B-1] 승인 정책이 COMPLETION/BOTH 인 경우에만 APPROVED 상태를 요구한다.
        // NONE 또는 ACTIVATION_ONLY 정책 목표는 goalApprovalStatus 가 NOT_REQUESTED 이므로 곧바로 완료 가능해야 한다.
        com._team._team.goal.domain.KpiTemplate tpl = goal.getKpiTemplate();
        com._team._team.goal.domain.enums.GoalApprovalPolicy policy = tpl != null
                ? tpl.resolveApprovalPolicy()
                : com._team._team.goal.domain.enums.GoalApprovalPolicy.NONE;
        if (policy.requiresCompletion()
                && goal.getGoalApprovalStatus() != GoalApprovalStatus.APPROVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "완료 처리는 승인 완료 후 가능합니다. 먼저 완료 승인 요청을 진행해 주세요.");
        }
        goal.complete();
        goalRollupService.recalculateFrom(goal.getGoalId());
        goalActivityService.append(goal.getGoalId(), GoalActivityType.APPROVED, requesterCompanyId, "목표 완료", null);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto cancelGoal(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "이미 완료되거나 취소된 목표는 취소할 수 없습니다. 현재 상태: " + goal.getStatus());
        }
        goal.cancel();
        goalRollupService.recalculateFrom(goal.getGoalId());
        goalActivityService.append(goal.getGoalId(), GoalActivityType.REJECTED, requesterCompanyId, "목표 취소", null);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto recalculate(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        validateSameCompany(goal.getCompanyId(), requesterCompanyId);
        goalRollupService.recalculateFrom(goalId);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto submitCompletionForApproval(
            UUID goalId,
            GoalCompletionSubmitReqDto dto,
            UUID requesterId,
            UUID requesterCompanyId,
            boolean hasGoalUpdatePermission) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        validateSameCompany(goal.getCompanyId(), requesterCompanyId);
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중(ACTIVE) 목표만 완료 제출할 수 있습니다.");
        }

        // [완료 승인 요청 권한]
        //   - MEMBER 목표: ownerId == requesterId
        //   - TEAM 목표: responsibleMemberId == requesterId
        //                || participantMemberIds 에 포함
        //                || hasGoalUpdatePermission (운영 우회)
        //   운영 우회(GOAL:UPDATE)는 TEAM 목표에만 허용 — MEMBER 목표는 본인만 선언 가능.
        if (!canRequestCompletion(goal, requesterId, hasGoalUpdatePermission)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    goal.getOwnerType() == GoalOwnerType.ORGANIZATION
                            ? "팀 목표 책임자 또는 참여자만 완료 승인 요청을 제출할 수 있습니다."
                            : "목표 당사자(소유자)만 완료 승인 요청을 제출할 수 있습니다.");
        }
        if (goal.getGoalApprovalStatus() == GoalApprovalStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 승인 요청이 진행 중입니다.");
        }

        // [승인자 결정] DTO.approverId > Goal.completionApproverId > 400
        //   - 목표 생성 시 이미 지정된 승인자(goal.completionApproverId)가 있으면 그대로 사용
        //   - DTO 로 오버라이드된 값이 오면 그 값 사용 + Goal 엔티티에도 갱신 (향후 다시 승인 요청 시 재사용)
        //   - 둘 다 없으면 400
        UUID resolvedApproverId = dto.getApproverId() != null
                ? dto.getApproverId()
                : goal.getCompletionApproverId();
        if (resolvedApproverId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "종료 승인자가 지정되지 않았습니다. 목표 생성 시 지정하거나 완료 요청 시 명시해주세요.");
        }
        if (dto.getApproverId() != null
                && !dto.getApproverId().equals(goal.getCompletionApproverId())) {
            // 사용자가 명시적으로 바꿨으면 향후 기본값으로도 사용되도록 Goal 엔티티에 반영.
            goal.updateCompletionApproverId(dto.getApproverId());
        }

        GoalApprovalRequestCreateReqDto ar = new GoalApprovalRequestCreateReqDto();
        ar.setApproverId(resolvedApproverId);
        goalApprovalService.request(goalId, requesterId, requesterCompanyId, ar, dto.getSummary(), dto.getEvidenceFiles());
        String summary = (dto.getSummary() != null && !dto.getSummary().isBlank())
                ? dto.getSummary().trim()
                : "목표 완료 제출이 등록되었습니다.";
        goalActivityService.append(goalId, GoalActivityType.COMPLETION_SUBMITTED, requesterId, summary, null);
        return GoalResDto.from(goalRepository.findById(goalId).orElse(goal));
    }

    /**
     * candidateParentId 가 ancestorGoalId 의 자손인지 여부를 BFS 로 확인.
     * 자기 자신을 부모로 지정하는 경우와 사이클을 예방합니다.
     */
    private boolean isDescendant(UUID ancestorGoalId, UUID candidateParentId) {
        if (ancestorGoalId == null || candidateParentId == null) return false;
        java.util.ArrayDeque<UUID> stack = new java.util.ArrayDeque<>();
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        stack.push(ancestorGoalId);
        visited.add(ancestorGoalId);
        int steps = 0;
        while (!stack.isEmpty() && steps < 1024) {
            UUID current = stack.pop();
            List<Goal> children = goalRepository.findByParentGoal_GoalId(current);
            for (Goal child : children) {
                UUID cid = child.getGoalId();
                if (cid.equals(candidateParentId)) return true;
                if (visited.add(cid)) {
                    stack.push(cid);
                }
            }
            steps++;
        }
        return false;
    }

    private void validateSameCompany(UUID goalCompanyId, UUID requesterCompanyId) {
        if (!goalCompanyId.equals(requesterCompanyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 목표에 접근할 수 없습니다.");
        }
    }

    private boolean isVisible(Goal goal, UUID requesterId) {
        boolean participant = goal.getParticipantMemberIds().contains(requesterId);
        return switch (goal.getVisibility()) {
            case PUBLIC, TEAM_ONLY -> true;
            case PRIVATE -> goal.getOwnerId().equals(requesterId) || participant;
        };
    }

    /**
     * 수동 진행률(비-자동집계) 권한.
     *   - MEMBER 목표: owner 본인
     *   - TEAM 목표  : 책임자 또는 참여 멤버
     *   (공개 범위상 조회 가능해야 함.  그 외에는 GOAL:UPDATE 권한으로 처리)
     */
    public boolean canMemberPostManualProgress(Goal goal, UUID memberId) {
        if (memberId == null) return false;
        if (goal.getOwnerType() == GoalOwnerType.MEMBER && goal.getOwnerId().equals(memberId)) {
            return isVisible(goal, memberId);
        }
        if (goal.getOwnerType() == GoalOwnerType.ORGANIZATION
                && memberId.equals(goal.getResponsibleMemberId())) {
            return isVisible(goal, memberId);
        }
        if (goal.getParticipantMemberIds().contains(memberId)) {
            return isVisible(goal, memberId);
        }
        return false;
    }

    // ─────────────────────────── TEAM 목표 권한 helper ───────────────────────────

    /**
     * 생성 시 ownerType 과 책임자 지정을 교차 검증.
     *   - MEMBER 목표: 책임자 지정은 무시 (null 반환)
     *   - TEAM 목표  : responsibleMemberId 필수
     */
    private UUID resolveResponsibleMemberIdForCreate(GoalCreateReqDto dto) {
        if (dto.getOwnerType() == GoalOwnerType.ORGANIZATION) {
            if (dto.getResponsibleMemberId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "팀(조직) 목표는 책임자(responsibleMemberId) 지정이 필수입니다.");
            }
            return dto.getResponsibleMemberId();
        }
        return null; // MEMBER 목표는 ownerId 자체가 책임자
    }

    /**
     * 완료 승인 요청 가능 여부.
     *   - MEMBER 목표: ownerId == requesterId (GOAL:UPDATE 우회 없음)
     *   - TEAM 목표  : 책임자 또는 참여자 || hasGoalUpdatePermission
     */
    private boolean canRequestCompletion(Goal goal, UUID requesterId, boolean hasGoalUpdatePermission) {
        if (requesterId == null) return false;
        if (goal.getOwnerType() == GoalOwnerType.MEMBER) {
            return goal.getOwnerId().equals(requesterId);
        }
        // TEAM
        if (requesterId.equals(goal.getResponsibleMemberId())) return true;
        if (goal.getParticipantMemberIds() != null
                && goal.getParticipantMemberIds().contains(requesterId)) return true;
        return hasGoalUpdatePermission;
    }
}
