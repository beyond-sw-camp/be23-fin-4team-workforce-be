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
import com._team._team.goal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoalApprovalService {

    private final GoalRepository goalRepository;
    private final GoalApprovalBundleRepository bundleRepository;
    private final GoalApprovalBundleGoalRepository bundleGoalRepository;
    private final GoalActivityService goalActivityService;
    private final GoalRollupService goalRollupService;

    public GoalApprovalService(
            GoalRepository goalRepository,
            GoalApprovalBundleRepository bundleRepository,
            GoalApprovalBundleGoalRepository bundleGoalRepository,
            GoalActivityService goalActivityService,
            GoalRollupService goalRollupService) {
        this.goalRepository = goalRepository;
        this.bundleRepository = bundleRepository;
        this.bundleGoalRepository = bundleGoalRepository;
        this.goalActivityService = goalActivityService;
        this.goalRollupService = goalRollupService;
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
        GoalApprovalBundle bundle = bundleRepository.save(
                GoalApprovalBundle.builder()
                        .companyId(companyId)
                        .requestedBy(requesterId)
                        .approverId(dto.getApproverId())
                        .requestedAt(LocalDateTime.now())
                        .status(BundleApprovalStatus.PENDING)
                        .decision(GoalApprovalDecision.PENDING)
                        .completionSummary(trimToNull(completionSummary))
                        .completionEvidenceFiles(trimToNull(completionEvidenceFiles))
                        .build());
        for (UUID gid : goalIds) {
            Goal goal = getGoal(gid, companyId);
            if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "완료/취소된 목표는 승인 요청할 수 없습니다. goalId=" + gid);
            }
            bundleGoalRepository.save(
                    GoalApprovalBundleGoal.builder().bundle(bundle).goal(goal).build());
            goal.updateGoalApprovalStatus(GoalApprovalStatus.PENDING);
        }
        for (UUID gid : goalIds) {
            goalActivityService.append(gid, GoalActivityType.APPROVAL_REQUESTED, requesterId, "승인 요청이 등록되었습니다.", null);
        }
        return get(goalIds.get(0), companyId);
    }

    @Transactional(readOnly = true)
    public GoalApprovalResDto get(UUID goalId, UUID companyId) {
        Goal goal = getGoal(goalId, companyId);
        return bundleGoalRepository.findFirstByGoal_GoalIdOrderByBundle_RequestedAtDesc(goalId)
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
                .approverId(bundle.getApproverId())
                .decision(bundle.getDecision())
                .decidedAt(bundle.getDecidedAt())
                .comment(bundle.getCommentText())
                .completionSummary(bundle.getCompletionSummary())
                .completionEvidenceFiles(bundle.getCompletionEvidenceFiles())
                .watchers(bundle.getWatcherIds().stream().map(GoalApprovalWatcherResDto::from).toList())
                .build();
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
        String note = dto != null ? dto.resolveNote() : null;
        bundle.approve(note);
        List<Goal> goals = goalsInBundle(bundle.getBundleId());
        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.DRAFT) {
                g.activate(approverId);
            } else if (g.getStatus() == GoalStatus.ACTIVE) {
                g.complete();
            }
            g.updateGoalApprovalStatus(GoalApprovalStatus.APPROVED);
            goalRollupService.recalculateFrom(g.getGoalId());
            goalActivityService.append(
                    g.getGoalId(),
                    GoalActivityType.APPROVED,
                    approverId,
                    g.getStatus() == GoalStatus.COMPLETED ? "목표 완료 승인" : "목표 활성 승인",
                    null);
        }
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
        List<GoalApprovalBundle> asApprover =
                bundleRepository.findByCompanyIdAndStatusAndDecisionAndApproverIdOrderByRequestedAtDesc(
                        companyId, BundleApprovalStatus.PENDING, GoalApprovalDecision.PENDING, memberId);
        List<GoalApprovalBundle> asRequester =
                bundleRepository.findByCompanyIdAndStatusAndDecisionAndRequestedByOrderByRequestedAtDesc(
                        companyId, BundleApprovalStatus.PENDING, GoalApprovalDecision.PENDING, memberId);
        Map<UUID, GoalApprovalBundle> dedup = new LinkedHashMap<>();
        for (GoalApprovalBundle b : asApprover) dedup.put(b.getBundleId(), b);
        for (GoalApprovalBundle b : asRequester) dedup.putIfAbsent(b.getBundleId(), b);
        List<GoalApprovalBundle> merged = new ArrayList<>(dedup.values());
        merged.sort((a, b) -> {
            LocalDateTime at = a.getRequestedAt();
            LocalDateTime bt = b.getRequestedAt();
            if (at == null && bt == null) return 0;
            if (at == null) return 1;
            if (bt == null) return -1;
            return bt.compareTo(at);
        });
        return merged;
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundleSummaryResDto> listPendingSummariesForApprover(UUID companyId, UUID memberId) {
        return listPendingForMember(companyId, memberId).stream()
                .map(b -> GoalApprovalBundleSummaryResDto.builder()
                        .requestId(b.getBundleId())
                        .status(b.getStatus().name().toLowerCase())
                        .goalCount(bundleGoalRepository.findByBundle_BundleId(b.getBundleId()).size())
                        .requestedAt(b.getRequestedAt())
                        .completionSummary(b.getCompletionSummary())
                        .completionEvidenceFiles(b.getCompletionEvidenceFiles())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoalApprovalBundleSummaryResDto> listHistorySummariesForMember(UUID companyId, UUID memberId) {
        List<GoalApprovalBundle> asApprover =
                bundleRepository.findByCompanyIdAndApproverIdOrderByRequestedAtDesc(companyId, memberId);
        List<GoalApprovalBundle> asRequester =
                bundleRepository.findByCompanyIdAndRequestedByOrderByRequestedAtDesc(companyId, memberId);
        Map<UUID, GoalApprovalBundle> dedup = new LinkedHashMap<>();
        for (GoalApprovalBundle b : asApprover) dedup.put(b.getBundleId(), b);
        for (GoalApprovalBundle b : asRequester) dedup.putIfAbsent(b.getBundleId(), b);
        List<GoalApprovalBundle> merged = new ArrayList<>(dedup.values());
        merged.sort((a, b) -> {
            LocalDateTime at = a.getRequestedAt();
            LocalDateTime bt = b.getRequestedAt();
            if (at == null && bt == null) return 0;
            if (at == null) return 1;
            if (bt == null) return -1;
            return bt.compareTo(at);
        });
        return merged.stream()
                .map(b -> GoalApprovalBundleSummaryResDto.builder()
                        .requestId(b.getBundleId())
                        .status(b.getStatus().name().toLowerCase())
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
