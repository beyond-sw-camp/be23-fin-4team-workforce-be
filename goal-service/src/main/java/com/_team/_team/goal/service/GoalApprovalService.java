package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.*;
import com._team._team.goal.domain.enums.*;
import com._team._team.goal.dto.reqdto.GoalApprovalDecisionReqDto;
import com._team._team.goal.dto.reqdto.GoalApprovalRequestCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalApprovalWatchersReqDto;
import com._team._team.goal.dto.resdto.GoalApprovalBundleDetailResDto;
import com._team._team.goal.dto.resdto.GoalApprovalBundleSummaryResDto;
import com._team._team.goal.dto.resdto.GoalApprovalResDto;
import com._team._team.goal.dto.resdto.GoalApprovalWatcherResDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.dto.NotificationMessage;
import com._team._team.goal.repository.*;
import com._team._team.notification.NotificationType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GoalApprovalService {

    private final GoalRepository goalRepository;
    private final GoalApprovalBundleRepository bundleRepository;
    private final GoalApprovalBundleGoalRepository bundleGoalRepository;
    private final GoalActivityService goalActivityService;
    private final GoalRollupService goalRollupService;
    private final ApplicationEventPublisher eventPublisher;
    private final GoalService goalService;

    public GoalApprovalService(
            GoalRepository goalRepository,
            GoalApprovalBundleRepository bundleRepository,
            GoalApprovalBundleGoalRepository bundleGoalRepository,
            GoalActivityService goalActivityService,
            GoalRollupService goalRollupService,
            ApplicationEventPublisher eventPublisher,
            @Lazy GoalService goalService) {
        this.goalRepository = goalRepository;
        this.bundleRepository = bundleRepository;
        this.bundleGoalRepository = bundleGoalRepository;
        this.goalActivityService = goalActivityService;
        this.goalRollupService = goalRollupService;
        this.eventPublisher = eventPublisher;
        this.goalService = goalService;
    }

    @Transactional
    public GoalApprovalResDto request(UUID goalId, UUID requesterId, UUID companyId, GoalApprovalRequestCreateReqDto dto) {
        return createBundleForGoals(List.of(goalId), requesterId, companyId, dto, null, null);
    }

    @Transactional
    public GoalApprovalResDto request(
            UUID goalId,
            UUID requesterId,
            UUID companyId,
            GoalApprovalRequestCreateReqDto dto,
            String completionSummary,
            String completionEvidenceFiles) {
        return createBundleForGoals(
                List.of(goalId),
                requesterId,
                companyId,
                dto,
                completionSummary,
                completionEvidenceFiles);
    }

    /**
     * KPI 일괄 생성 등 다목표 단일 번들 승인에 사용합니다.
     */
    @Transactional
    public GoalApprovalResDto createBundleForGoals(
            List<UUID> goalIds, UUID requesterId, UUID companyId, GoalApprovalRequestCreateReqDto dto) {
        return createBundleForGoals(goalIds, requesterId, companyId, dto, null, null);
    }

    @Transactional
    public GoalApprovalResDto createBundleForGoals(
            List<UUID> goalIds,
            UUID requesterId,
            UUID companyId,
            GoalApprovalRequestCreateReqDto dto,
            String completionSummary,
            String completionEvidenceFiles) {
        if (goalIds == null || goalIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "승인 대상 목표가 없습니다.");
        }
        if (dto == null || dto.getApproverId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "승인자가 지정되지 않았습니다.");
        }
        if (dto.getApproverId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "완료 승인자는 요청자 본인으로 지정할 수 없습니다.");
        }

        // 목표들의 현재 상태로부터 approvalKind 를 유도한다.
        // - 모두 DRAFT  → ACTIVATION
        // - 모두 ACTIVE → COMPLETION
        // - 섞여 있으면 → BAD_REQUEST (같은 번들로 묶을 수 없음)
        // 이후 KpiTemplate.goalApprovalPolicy 를 교차 검증해 불필요한 번들 생성 자체를 차단한다.
        List<Goal> goalsForKind = goalIds.stream().map(gid -> getGoal(gid, companyId)).toList();
        BundleApprovalKind kind = deriveApprovalKind(goalsForKind);
        enforcePolicyForGoals(goalsForKind, kind);

        GoalApprovalBundle bundle = bundleRepository.save(
                GoalApprovalBundle.builder()
                        .companyId(companyId)
                        .requestedBy(requesterId)
                        .approverId(dto.getApproverId())
                        .requestedAt(LocalDateTime.now())
                        .status(BundleApprovalStatus.PENDING)
                        .decision(GoalApprovalDecision.PENDING)
                        .approvalKind(kind)
                        .completionSummary(trimToNull(completionSummary))
                        .completionEvidenceFiles(trimToNull(completionEvidenceFiles))
                        .build());
        for (Goal goal : goalsForKind) {
            if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "완료/취소된 목표는 승인 요청할 수 없습니다. goalId=" + goal.getGoalId());
            }
            bundleGoalRepository.save(
                    GoalApprovalBundleGoal.builder().bundle(bundle).goal(goal).build());
            goal.updateGoalApprovalStatus(GoalApprovalStatus.PENDING);
        }
        for (UUID gid : goalIds) {
            goalActivityService.append(gid, GoalActivityType.APPROVAL_REQUESTED, requesterId, "승인 요청이 등록되었습니다.", null);
        }
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(dto.getApproverId())
                .senderId(requesterId)
                .notificationType(NotificationType.APPROVAL_REQUESTED)
                .content(goalIds.size() > 1
                        ? "목표 " + goalIds.size() + "건에 대한 승인 요청이 도착했습니다."
                        : "목표 승인 요청이 도착했습니다.")
                .targetId(bundle.getBundleId())
                .targetType("GOAL_APPROVAL_BUNDLE")
                .build());
        return get(goalIds.get(0), companyId, requesterId);
    }

    @Transactional(readOnly = true)
    public GoalApprovalResDto get(UUID goalId, UUID companyId, UUID readerId) {
        Goal goal = getGoal(goalId, companyId);
        var linkOpt = bundleGoalRepository.findFirstByGoal_GoalIdOrderByBundle_RequestedAtDesc(goalId);
        GoalApprovalBundle bundle = linkOpt.map(GoalApprovalBundleGoal::getBundle).orElse(null);
        if (!goalService.isVisibleToMember(goal, readerId)) {
            boolean allowed = bundle != null && (readerId.equals(bundle.getApproverId())
                    || readerId.equals(bundle.getRequestedBy())
                    || bundle.getWatcherIds().contains(readerId));
            if (!allowed) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "해당 목표 승인 정보에 대한 접근 권한이 없습니다.");
            }
        }
        return linkOpt
                .map(link -> toResDto(goalId, link.getBundle()))
                .orElseGet(() -> GoalApprovalResDto.builder()
                        .goalId(goalId)
                        .requestId(null)
                        .approvalStatus(goal.getGoalApprovalStatus())
                        .approverId(null)
                        .decision(null)
                        .decidedAt(null)
                        .comment(null)
                        .watchers(List.of())
                        .build());
    }

    private GoalApprovalResDto toResDto(UUID goalId, GoalApprovalBundle bundle) {
        GoalApprovalStatus st = mapBundleStatus(bundle);
        return GoalApprovalResDto.builder()
                .goalId(goalId)
                .requestId(bundle.getBundleId())
                .approvalStatus(st)
                .approvalKind(bundle.getApprovalKind())
                .approverId(bundle.getApproverId())
                .decision(bundle.getDecision())
                .decidedAt(bundle.getDecidedAt())
                .comment(bundle.getCommentText())
                .completionSummary(bundle.getCompletionSummary())
                .completionEvidenceFiles(bundle.getCompletionEvidenceFiles())
                .watchers(bundle.getWatcherIds().stream().map(GoalApprovalWatcherResDto::from).toList())
                .build();
    }

    /**
     * 번들의 목표 상태들로부터 활성화/종료 구분을 유도한다.
     * 모두 DRAFT → ACTIVATION, 모두 ACTIVE → COMPLETION, 섞여 있으면 400 에러.
     */
    private static BundleApprovalKind deriveApprovalKind(List<Goal> goals) {
        boolean hasDraft = goals.stream().anyMatch(g -> g.getStatus() == GoalStatus.DRAFT);
        boolean hasActive = goals.stream().anyMatch(g -> g.getStatus() == GoalStatus.ACTIVE);
        if (hasDraft && hasActive) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 와 ACTIVE 목표를 한 승인 번들로 묶을 수 없습니다. 활성화 승인과 종료 승인은 분리해주세요.");
        }
        if (!hasDraft && !hasActive) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 또는 ACTIVE 상태의 목표만 승인 요청할 수 있습니다.");
        }
        return hasDraft ? BundleApprovalKind.ACTIVATION : BundleApprovalKind.COMPLETION;
    }

    /**
     * KpiTemplate 에 설정된 GoalApprovalPolicy / 기존 requireApproval 플래그를 읽어
     * 해당 단계(activation/completion)에 승인 절차가 필요한지 검증한다.
     * 템플릿이 없는 목표는 검사를 건너뛴다.
     */
    private static void enforcePolicyForGoals(List<Goal> goals, BundleApprovalKind kind) {
        for (Goal g : goals) {
            KpiTemplate tpl = g.getKpiTemplate();
            if (tpl == null) continue;

            GoalApprovalPolicy policy = tpl.resolveApprovalPolicy();
            if (!policy.requires(kind)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "해당 KPI 템플릿(" + tpl.getName() + ")은 " + kind.name()
                                + " 승인을 요구하지 않습니다. 정책=" + policy.name());
            }
        }
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static GoalApprovalStatus mapBundleStatus(GoalApprovalBundle bundle) {
        if (bundle.getStatus() == BundleApprovalStatus.APPROVED) return GoalApprovalStatus.APPROVED;
        if (bundle.getStatus() == BundleApprovalStatus.REJECTED) return GoalApprovalStatus.REJECTED;
        return GoalApprovalStatus.PENDING;
    }

    @Transactional
    public GoalApprovalResDto approve(UUID goalId, UUID bundleId, UUID approverId, UUID companyId, GoalApprovalDecisionReqDto dto) {
        Goal goal = getGoal(goalId, companyId);
        GoalApprovalBundle bundle = getBundle(bundleId, companyId);
        assertBundleContainsGoal(bundle, goalId);
        if (bundle.getStatus() != BundleApprovalStatus.PENDING || bundle.getDecision() != GoalApprovalDecision.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 결재 요청이 아닙니다.");
        }
        if (!bundle.getApproverId().equals(approverId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "지정된 승인자가 아닙니다.");
        }

        // 번들 내 모든 목표를 선 검증 — 하나라도 상태 이상이 있으면 전부 롤백
        List<Goal> goals = goalsInBundle(bundle.getBundleId());
        if (goals.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "번들에 목표가 없습니다.");
        }
        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.COMPLETED || g.getStatus() == GoalStatus.CANCELLED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "완료/취소된 목표가 번들에 포함되어 있어 승인할 수 없습니다. goalId=" + g.getGoalId());
            }
            if (g.getGoalApprovalStatus() != GoalApprovalStatus.PENDING) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "결재 진행 상태가 아닌 목표가 번들에 포함되어 있습니다. goalId=" + g.getGoalId());
            }
        }

        String note = dto != null ? dto.resolveNote() : null;
        bundle.approve(note);

        // 상태 전이는 검증 후 일괄 수행
        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.DRAFT) {
                g.activate(approverId);
            } else if (g.getStatus() == GoalStatus.ACTIVE) {
                g.complete();
            }
            g.updateGoalApprovalStatus(GoalApprovalStatus.APPROVED);
        }
        // Rollup 은 상태 전이 후 한 번에 — 같은 부모를 공유할 경우 중복 호출이 있어도
        // idempotent 하도록 설계돼 있으므로 수용 가능.
        for (Goal g : goals) {
            goalRollupService.recalculateFrom(g.getGoalId());
            goalActivityService.append(
                    g.getGoalId(),
                    GoalActivityType.APPROVED,
                    approverId,
                    g.getStatus() == GoalStatus.COMPLETED ? "목표 완료 승인" : "목표 활성 승인",
                    null);
        }
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(bundle.getRequestedBy())
                .senderId(approverId)
                .notificationType(NotificationType.APPROVAL_APPROVED)
                .content("목표 승인이 완료되었습니다.")
                .targetId(bundle.getBundleId())
                .targetType("GOAL_APPROVAL_BUNDLE")
                .build());
        return get(goalId, companyId);
    }

    @Transactional
    public GoalApprovalResDto reject(UUID goalId, UUID bundleId, UUID approverId, UUID companyId, GoalApprovalDecisionReqDto dto) {
        Goal goal = getGoal(goalId, companyId);
        GoalApprovalBundle bundle = getBundle(bundleId, companyId);
        assertBundleContainsGoal(bundle, goalId);
        if (bundle.getStatus() != BundleApprovalStatus.PENDING || bundle.getDecision() != GoalApprovalDecision.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 결재 요청이 아닙니다.");
        }
        if (!bundle.getApproverId().equals(approverId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "지정된 승인자가 아닙니다.");
        }
        String reason = dto != null ? dto.resolveNote() : null;
        bundle.reject(reason != null && !reason.isBlank() ? reason : "반려되었습니다.");
        for (Goal g : goalsInBundle(bundle.getBundleId())) {
            g.updateGoalApprovalStatus(GoalApprovalStatus.REJECTED);
            goalActivityService.append(g.getGoalId(), GoalActivityType.REJECTED, approverId, "목표 승인 반려", reason);
        }
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(bundle.getRequestedBy())
                .senderId(approverId)
                .notificationType(NotificationType.APPROVAL_REJECTED)
                .content(reason != null && !reason.isBlank()
                        ? "목표 승인이 반려되었습니다. 사유: " + reason
                        : "목표 승인이 반려되었습니다.")
                .targetId(bundle.getBundleId())
                .targetType("GOAL_APPROVAL_BUNDLE")
                .build());
        return get(goalId, companyId);
    }

    @Transactional
    public GoalApprovalResDto addWatchers(UUID goalId, UUID bundleId, UUID companyId, GoalApprovalWatchersReqDto dto) {
        getGoal(goalId, companyId);
        GoalApprovalBundle bundle = getBundle(bundleId, companyId);
        assertBundleContainsGoal(bundle, goalId);
        bundle.addWatchers(dto.getWatcherIds());
        return get(goalId, companyId);
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundle> listPendingForMember(UUID companyId, UUID memberId) {
        // "내가 처리할 승인"은 지정 승인자(approver)인 요청만 반환한다.
        return bundleRepository.findByCompanyIdAndStatusAndDecisionAndApproverIdOrderByRequestedAtDesc(
                companyId, BundleApprovalStatus.PENDING, GoalApprovalDecision.PENDING, memberId);
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundle> listRequestedHistoryForMember(UUID companyId, UUID memberId) {
        // "내가 보낸 요청"은 요청자(requestedBy)인 건만 상태와 무관하게 반환한다.
        return bundleRepository.findByCompanyIdAndRequestedByOrderByRequestedAtDesc(companyId, memberId);
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundleSummaryResDto> listPendingSummariesForApprover(UUID companyId, UUID memberId) {
        return listPendingForMember(companyId, memberId).stream()
                .map(b -> GoalApprovalBundleSummaryResDto.builder()
                        .requestId(b.getBundleId())
                        .status(b.getStatus().name().toLowerCase())
                        .approvalKind(b.getApprovalKind())
                        .goalCount(bundleGoalRepository.findByBundle_BundleId(b.getBundleId()).size())
                        .requestedAt(b.getRequestedAt())
                        .completionSummary(b.getCompletionSummary())
                        .completionEvidenceFiles(b.getCompletionEvidenceFiles())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundleSummaryResDto> listHistorySummariesForMember(UUID companyId, UUID memberId) {
        return listRequestedHistoryForMember(companyId, memberId).stream()
                .map(b -> GoalApprovalBundleSummaryResDto.builder()
                        .requestId(b.getBundleId())
                        .status(b.getStatus().name().toLowerCase())
                        .approvalKind(b.getApprovalKind())
                        .goalCount(bundleGoalRepository.findByBundle_BundleId(b.getBundleId()).size())
                        .requestedAt(b.getRequestedAt())
                        .completionSummary(b.getCompletionSummary())
                        .completionEvidenceFiles(b.getCompletionEvidenceFiles())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalApprovalBundleDetailResDto getBundleDetail(UUID bundleId, UUID companyId, UUID readerId) {
        GoalApprovalBundle bundle = getBundle(bundleId, companyId);
        if (bundle.getStatus() == BundleApprovalStatus.PENDING && !isReaderInvolved(bundle, readerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "승인 요청을 조회할 권한이 없습니다.");
        }
        List<Goal> goals = goalsInBundle(bundleId);
        List<GoalResDto> goalRows = goals.stream().map(GoalResDto::from).toList();
        return GoalApprovalBundleDetailResDto.builder()
                .requestId(bundleId)
                .status(bundle.getStatus().name().toLowerCase())
                .approvalKind(bundle.getApprovalKind())
                .rejectionReason(bundle.getRejectionReason())
                .goals(goalRows)
                .approverId(bundle.getApproverId())
                .decision(bundle.getDecision() != null ? bundle.getDecision().name() : null)
                .decidedAt(bundle.getDecidedAt())
                .comment(bundle.getCommentText())
                .completionSummary(bundle.getCompletionSummary())
                .completionEvidenceFiles(bundle.getCompletionEvidenceFiles())
                .watchers(bundle.getWatcherIds().stream().map(GoalApprovalWatcherResDto::from).toList())
                .build();
    }

    private boolean isReaderInvolved(GoalApprovalBundle bundle, UUID readerId) {
        if (bundle.getRequestedBy().equals(readerId)) return true;
        if (bundle.getApproverId().equals(readerId)) return true;
        return bundle.getWatcherIds().stream().anyMatch(w -> w.equals(readerId));
    }

    private List<Goal> goalsInBundle(UUID bundleId) {
        return bundleGoalRepository.findByBundle_BundleId(bundleId).stream()
                .map(GoalApprovalBundleGoal::getGoal)
                .toList();
    }

    private Goal getGoal(UUID goalId, UUID companyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표를 찾을 수 없습니다. id=" + goalId));
        if (!goal.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 목표입니다.");
        }
        return goal;
    }

    private GoalApprovalBundle getBundle(UUID bundleId, UUID companyId) {
        GoalApprovalBundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "승인 요청을 찾을 수 없습니다."));
        if (!bundle.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 승인 요청입니다.");
        }
        return bundle;
    }

    private void assertBundleContainsGoal(GoalApprovalBundle bundle, UUID goalId) {
        boolean ok = bundleGoalRepository.findByBundle_BundleId(bundle.getBundleId()).stream()
                .anyMatch(l -> l.getGoal().getGoalId().equals(goalId));
        if (!ok) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "요청에 포함된 목표가 아닙니다.");
        }
    }

    @Transactional
    public GoalApprovalBundleDetailResDto approveBundleOnly(
            UUID bundleId, UUID approverId, UUID companyId, GoalApprovalDecisionReqDto dto) {
        List<Goal> gs = goalsInBundle(bundleId);
        if (gs.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "번들에 목표가 없습니다.");
        }
        approve(gs.get(0).getGoalId(), bundleId, approverId, companyId, dto);
        return getBundleDetail(bundleId, companyId, approverId);
    }

    @Transactional
    public GoalApprovalBundleDetailResDto rejectBundleOnly(
            UUID bundleId, UUID approverId, UUID companyId, GoalApprovalDecisionReqDto dto) {
        List<Goal> gs = goalsInBundle(bundleId);
        if (gs.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "번들에 목표가 없습니다.");
        }
        reject(gs.get(0).getGoalId(), bundleId, approverId, companyId, dto);
        return getBundleDetail(bundleId, companyId, approverId);
    }
}