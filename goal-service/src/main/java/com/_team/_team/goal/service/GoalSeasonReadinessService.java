package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.GoalApprovalBundle;
import com._team._team.goal.domain.enums.BundleApprovalStatus;
import com._team._team.goal.domain.enums.GoalActivityType;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.dto.resdto.GoalSeasonReadinessResDto;
import com._team._team.goal.repository.GoalActivityRepository;
import com._team._team.goal.repository.GoalApprovalBundleRepository;
import com._team._team.goal.repository.GoalRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GoalSeasonReadinessService {

    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationGroupRepository groupRepository;
    private final GoalRepository goalRepository;
    private final GoalApprovalBundleRepository bundleRepository;
    private final GoalActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    public GoalSeasonReadinessService(
            EvaluationSeasonRepository seasonRepository,
            EvaluationGroupRepository groupRepository,
            GoalRepository goalRepository,
            GoalApprovalBundleRepository bundleRepository,
            GoalActivityRepository activityRepository,
            ObjectMapper objectMapper) {
        this.seasonRepository = seasonRepository;
        this.groupRepository = groupRepository;
        this.goalRepository = goalRepository;
        this.bundleRepository = bundleRepository;
        this.activityRepository = activityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GoalSeasonReadinessResDto inspect(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 평가 시즌입니다.");
        }
        if (season.getTargetCycle() == null || season.getTargetCycleStart() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "평가 시즌의 목표 사이클이 설정되어 있지 않습니다.");
        }

        List<EvaluationGroup> groups = groupRepository.findBySeason_SeasonId(seasonId);
        Set<UUID> targetMemberIds = extractTargetMemberIds(groups);

        List<GoalSeasonReadinessResDto.MemberIssue> missingGoals = new ArrayList<>();
        List<GoalSeasonReadinessResDto.MemberIssue> weightIssues = new ArrayList<>();
        List<GoalSeasonReadinessResDto.MemberIssue> pendingBundles = new ArrayList<>();
        List<GoalSeasonReadinessResDto.MemberIssue> missingProgressUpdates = new ArrayList<>();
        List<GoalSeasonReadinessResDto.MemberIssue> missingLeads = findMissingLeads(groups);
        int activeGoalCount = 0;

        for (UUID memberId : targetMemberIds) {
            List<Goal> activeGoals = goalRepository.findByOwnerIdAndCycleAndCycleStartDateAndStatusIn(
                    memberId,
                    season.getTargetCycle(),
                    season.getTargetCycleStart(),
                    EnumSet.of(GoalStatus.ACTIVE)
            );
            activeGoalCount += activeGoals.size();

            int weightSum = activeGoals.stream().mapToInt(Goal::getWeightPct).sum();
            if (activeGoals.isEmpty()) {
                missingGoals.add(issue(memberId, "평가 대상 사이클에 승인된 개인 목표가 없습니다.", weightSum, 0));
            } else if (weightSum != 100) {
                weightIssues.add(issue(memberId, "개인 목표 가중치 합계가 100%가 아닙니다.", weightSum, activeGoals.size()));
            }

            List<GoalApprovalBundle> pending = bundleRepository.findByRequestedByAndCompanyIdAndStatus(
                    memberId, companyId, BundleApprovalStatus.PENDING);
            if (!pending.isEmpty()) {
                pendingBundles.add(issue(memberId, "승인 대기 중인 목표 bundle이 남아 있습니다.", weightSum, activeGoals.size()));
            }

            boolean hasAnyProgressUpdate = activeGoals.stream()
                    .anyMatch(goal -> activityRepository.existsByGoal_GoalIdAndType(
                            goal.getGoalId(), GoalActivityType.PROGRESS_UPDATED));
            if (!activeGoals.isEmpty() && !hasAnyProgressUpdate) {
                missingProgressUpdates.add(issue(memberId, "승인 이후 진행률 업데이트 이력이 없습니다.", weightSum, activeGoals.size()));
            }
        }

        int blockerCount = missingGoals.size() + weightIssues.size() + pendingBundles.size() + missingLeads.size();
        int warningCount = missingProgressUpdates.size();

        return GoalSeasonReadinessResDto.builder()
                .seasonId(seasonId)
                .ready(blockerCount == 0)
                .targetMemberCount(targetMemberIds.size())
                .activeGoalCount(activeGoalCount)
                .blockerCount(blockerCount)
                .warningCount(warningCount)
                .missingGoals(missingGoals)
                .weightIssues(weightIssues)
                .pendingBundles(pendingBundles)
                .missingProgressUpdates(missingProgressUpdates)
                .missingLeads(missingLeads)
                .build();
    }

    private Set<UUID> extractTargetMemberIds(List<EvaluationGroup> groups) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (EvaluationGroup group : groups) {
            try {
                String json = group.getTargetMemberIdsJson();
                if (json != null && !json.isBlank()) {
                    ids.addAll(objectMapper.readValue(json, new TypeReference<List<UUID>>() {}));
                }
            } catch (Exception ignored) {
                // Invalid group JSON is handled by group editor validation.
            }
        }
        return ids;
    }

    private List<GoalSeasonReadinessResDto.MemberIssue> findMissingLeads(List<EvaluationGroup> groups) {
        List<GoalSeasonReadinessResDto.MemberIssue> issues = new ArrayList<>();
        for (EvaluationGroup group : groups) {
            Set<UUID> targets = new LinkedHashSet<>();
            try {
                String json = group.getTargetMemberIdsJson();
                if (json != null && !json.isBlank()) {
                    targets.addAll(objectMapper.readValue(json, new TypeReference<List<UUID>>() {}));
                }
            } catch (Exception ignored) {
                continue;
            }
            for (UUID targetId : targets) {
                boolean hasLead = group.getEvaluatorMaps() != null && group.getEvaluatorMaps().stream()
                        .anyMatch(map -> targetId.equals(map.getTargetMemberId())
                                && "DOWNWARD".equalsIgnoreCase(String.valueOf(map.getEvaluationType()))
                                && map.getEvaluatorId() != null
                                && !targetId.equals(map.getEvaluatorId()));
                if (!hasLead) {
                    issues.add(issue(targetId, "Lead 평가자가 지정되지 않았습니다.", null, null));
                }
            }
        }
        return issues;
    }

    private GoalSeasonReadinessResDto.MemberIssue issue(UUID memberId, String reason, Integer weightSum, Integer goalCount) {
        return GoalSeasonReadinessResDto.MemberIssue.builder()
                .memberId(memberId)
                .reason(reason)
                .weightSum(weightSum)
                .goalCount(goalCount)
                .build();
    }
}
