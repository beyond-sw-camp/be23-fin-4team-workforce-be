package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationCalibration;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.CalibrationRole;
import com._team._team.evaluation.domain.enums.EvaluationStage;
import com._team._team.evaluation.repository.EvaluationCalibrationRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalApprovalStatus;
import com._team._team.goal.domain.enums.GoalOwnerType;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
import com._team._team.goal.domain.enums.Grade;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalAggregateResDto;
import com._team._team.goal.dto.resdto.GoalKrResDto;
import com._team._team.goal.dto.resdto.GoalObjectiveResDto;
import com._team._team.goal.dto.resdto.GoalViewMapper;
import com._team._team.goal.dto.resdto.GoalViewResDto;
import com._team._team.goal.feignclients.MemberServiceClient;
import com._team._team.goal.repository.GoalRepository;
import com._team._team.goal.util.CycleKeyResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final MemberServiceClient memberServiceClient;
    private final EvaluationResponseRepository evaluationResponseRepository;
    private final EvaluationCalibrationRepository evaluationCalibrationRepository;
    private final ObjectMapper objectMapper;

    public GoalService(GoalRepository goalRepository,
                       MemberServiceClient memberServiceClient,
                       EvaluationResponseRepository evaluationResponseRepository,
                       EvaluationCalibrationRepository evaluationCalibrationRepository,
                       ObjectMapper objectMapper) {
        this.goalRepository = goalRepository;
        this.memberServiceClient = memberServiceClient;
        this.evaluationResponseRepository = evaluationResponseRepository;
        this.evaluationCalibrationRepository = evaluationCalibrationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GoalViewResDto create(GoalCreateReqDto dto, UUID requesterId, UUID companyId) {
        validateOwner(dto.getOwnerType(), dto.getOwnerId(), requesterId, companyId);
        validateCycleDates(dto.getCycle(), dto.getCycleStartDate(), dto.getCycleEndDate());
        validateAlignment(dto.getOwnerType(), dto.getAlignedOrgGoalId(), companyId);

        Goal goal = Goal.builder()
                .companyId(companyId)
                .ownerType(dto.getOwnerType())
                .ownerId(dto.getOwnerId())
                .alignedOrgGoalId(dto.getAlignedOrgGoalId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .cycle(dto.getCycle())
                .cycleStartDate(dto.getCycleStartDate())
                .cycleEndDate(dto.getCycleEndDate())
                .visibility(dto.getVisibility())
                .weightPct(dto.getOwnerType() == GoalOwnerType.ORGANIZATION ? 0 : dto.getWeightPct())
                .gradeSCriteria(dto.getOwnerType() == GoalOwnerType.ORGANIZATION ? dto.getGradeS() : null)
                .gradeACriteria(dto.getOwnerType() == GoalOwnerType.ORGANIZATION ? dto.getGradeA() : null)
                .gradeBCriteria(dto.getOwnerType() == GoalOwnerType.ORGANIZATION ? dto.getGradeB() : null)
                .gradeCCriteria(dto.getOwnerType() == GoalOwnerType.ORGANIZATION ? dto.getGradeC() : null)
                .visibleTeamIds(dto.getVisibleTeamIds() != null ? dto.getVisibleTeamIds() : new ArrayList<>())
                .participantMemberIds(dto.getParticipantMemberIds() != null ? dto.getParticipantMemberIds() : new ArrayList<>())
                .build();

        if (dto.getOwnerType() == GoalOwnerType.ORGANIZATION) {
            validateGradeCriteria(dto.getGradeS(), dto.getGradeA(), dto.getGradeB(), dto.getGradeC(), "Objective");
            goal.activateObjective();
        } else {
            validateKrDefinition(goal, companyId);
        }

        return GoalViewMapper.toView(goalRepository.save(goal));
    }

    @Transactional
    public GoalViewResDto update(UUID goalId, GoalUpdateReqDto dto, UUID requesterId, UUID companyId, boolean canManageOrgGoal) {
        Goal goal = mustGet(goalId, companyId);

        if (goal.getOwnerType() == GoalOwnerType.ORGANIZATION) {
            ensureObjectiveOwner(goal, requesterId, companyId, canManageOrgGoal);
            if (dto.getTitle() != null || dto.getDescription() != null) {
                goal.updateObjectiveContent(
                        dto.getTitle() != null ? dto.getTitle() : goal.getTitle(),
                        dto.getDescription() != null ? dto.getDescription() : goal.getDescription()
                );
            }
            if (dto.getVisibility() != null) {
                goal.updateObjectiveVisibility(dto.getVisibility(), dto.getVisibleTeamIds());
            }
            if (dto.getParticipantMemberIds() != null) {
                goal.updateObjectiveParticipants(dto.getParticipantMemberIds());
            }
            if (dto.getGradeS() != null || dto.getGradeA() != null || dto.getGradeB() != null || dto.getGradeC() != null) {
                String s = dto.getGradeS() != null ? dto.getGradeS() : goal.getGradeSCriteria();
                String a = dto.getGradeA() != null ? dto.getGradeA() : goal.getGradeACriteria();
                String b = dto.getGradeB() != null ? dto.getGradeB() : goal.getGradeBCriteria();
                String c = dto.getGradeC() != null ? dto.getGradeC() : goal.getGradeCCriteria();
                validateGradeCriteria(s, a, b, c, "Objective");
                goal.updateObjectiveGradeCriteria(s, a, b, c);
            }
            return GoalViewMapper.toView(goal);
        }

        if (goal.getStatus() != GoalStatus.DRAFT || goal.getGoalApprovalStatus() == GoalApprovalStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only draft KRs can be edited.");
        }
        if (!goal.getOwnerId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You can edit only your own KR.");
        }

        if (dto.getTitle() != null || dto.getDescription() != null) {
            goal.updateContent(
                    dto.getTitle() != null ? dto.getTitle() : goal.getTitle(),
                    dto.getDescription() != null ? dto.getDescription() : goal.getDescription()
            );
        }
        if (dto.getWeightPct() != null) {
            goal.updateWeight(dto.getWeightPct());
        }
        if (dto.getAlignedOrgGoalId() != null) {
            validateAlignment(goal.getOwnerType(), dto.getAlignedOrgGoalId(), companyId);
            goal.updateAlignment(dto.getAlignedOrgGoalId());
        }
        if (dto.getVisibility() != null) {
            goal.updateVisibility(dto.getVisibility(), dto.getVisibleTeamIds());
        }
        if (dto.getParticipantMemberIds() != null) {
            goal.updateParticipants(dto.getParticipantMemberIds());
        }
        if (dto.getGradeS() != null || dto.getGradeA() != null || dto.getGradeB() != null || dto.getGradeC() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KR grade criteria are inherited from the aligned objective.");
        }
        validateKrDefinition(goal, companyId);
        return GoalViewMapper.toView(goal);
    }

    @Transactional
    public void delete(UUID goalId, UUID requesterId, UUID companyId, boolean canManageOrgGoal) {
        Goal goal = mustGet(goalId, companyId);
        if (goal.getOwnerType() == GoalOwnerType.ORGANIZATION) {
            ensureObjectiveOwner(goal, requesterId, companyId, canManageOrgGoal);
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Objective deletion is not supported. End or cancel the objective instead."
            );
        } else {
            if (goal.getStatus() != GoalStatus.DRAFT) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Only draft KRs can be deleted.");
            }
            if (!goal.getOwnerId().equals(requesterId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "You can delete only your own KR.");
            }
        }
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalViewResDto cancelMine(UUID goalId, UUID requesterId, UUID companyId, boolean canManageOrgGoal) {
        Goal goal = mustGet(goalId, companyId);
        if (goal.getOwnerType() == GoalOwnerType.MEMBER) {
            if (!goal.getOwnerId().equals(requesterId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "You can cancel only your own KR.");
            }
        } else {
            ensureObjectiveOwner(goal, requesterId, companyId, canManageOrgGoal);
        }
        try {
            goal.cancel();
        } catch (IllegalStateException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return GoalViewMapper.toView(goal);
    }

    @Transactional
    public GoalViewResDto forceSkipByHr(UUID goalId, String reason, UUID companyId) {
        Goal goal = mustGet(goalId, companyId);
        goal.forceSkipByHr(reason);
        return GoalViewMapper.toView(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalKrResDto> listMyGoals(UUID memberId, UUID companyId, GoalStatus statusFilter) {
        List<Goal> goals = (statusFilter == null)
                ? goalRepository.findByCompanyIdAndOwnerIdOrderByCycleStartDateDesc(companyId, memberId)
                : goalRepository.findByCompanyIdAndOwnerIdAndStatus(companyId, memberId, statusFilter);
        return mapKrDtos(goals, companyId);
    }

    @Transactional(readOnly = true)
    public List<GoalViewResDto> listCompanyGoals(UUID requesterId, UUID companyId, KpiCycle cycle, boolean hasEvalRead) {
        return goalRepository.findByCompanyId(companyId).stream()
                .filter(g -> cycle == null || g.getCycle() == cycle)
                .filter(g -> isVisible(g, requesterId, companyId, hasEvalRead))
                .map(GoalViewMapper::toView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalObjectiveResDto> listMyObjectives(UUID requesterId, UUID companyId, KpiCycle cycle) {
        UUID requesterOrgId = memberServiceClient.findOrganizationId(companyId, requesterId);
        if (requesterOrgId == null) {
            return Collections.emptyList();
        }
        return goalRepository.findByCompanyIdAndOwnerTypeAndOwnerIdOrderByCycleStartDateDesc(
                        companyId, GoalOwnerType.ORGANIZATION, requesterOrgId)
                .stream()
                .filter(g -> cycle == null || g.getCycle() == cycle)
                .map(GoalObjectiveResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalObjectiveResDto> listAvailableObjectives(UUID requesterId, UUID companyId, KpiCycle cycle) {
        UUID requesterOrgId = memberServiceClient.findOrganizationId(companyId, requesterId);
        if (requesterOrgId == null) {
            return Collections.emptyList();
        }
        return goalRepository.findByCompanyIdAndOwnerTypeOrderByCycleStartDateDesc(companyId, GoalOwnerType.ORGANIZATION)
                .stream()
                .filter(g -> cycle == null || g.getCycle() == cycle)
                .filter(g -> requesterOrgId.equals(g.getOwnerId()))
                .filter(g -> isObjectiveAvailable(g, requesterOrgId))
                .map(GoalObjectiveResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalKrResDto> listObjectiveChildren(UUID objectiveId, UUID requesterId, UUID companyId, boolean hasEvalRead) {
        Goal objective = mustGet(objectiveId, companyId);
        if (objective.getOwnerType() != GoalOwnerType.ORGANIZATION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only organization objectives support child drilldown.");
        }
        if (!isVisible(objective, requesterId, companyId, hasEvalRead)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to read this objective.");
        }
        List<Goal> children = goalRepository.findByAlignedOrgGoalId(objectiveId).stream()
                .filter(goal -> goal.getOwnerType() == GoalOwnerType.MEMBER)
                .filter(goal -> requesterId.equals(goal.getOwnerId()))
                .filter(goal -> isVisible(goal, requesterId, companyId, hasEvalRead))
                .collect(Collectors.toList());
        return mapKrDtos(children, companyId);
    }

    @Transactional(readOnly = true)
    public List<GoalKrResDto> listOrgGoals(UUID callerId, UUID orgId, UUID companyId) {
        List<UUID> memberIds = memberServiceClient.findMemberIdsByOrgId(companyId, orgId);
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<GoalVisibility> visibilities = EnumSet.allOf(GoalVisibility.class);
        return mapKrDtos(goalRepository.findOrgScope(companyId, GoalOwnerType.MEMBER, memberIds, visibilities), companyId);
    }

    @Transactional(readOnly = true)
    public List<GoalObjectiveResDto> listOrgObjectives(UUID callerId, UUID orgId, UUID companyId, KpiCycle cycle) {
        return goalRepository.findByCompanyIdAndOwnerTypeAndOwnerIdOrderByCycleStartDateDesc(
                        companyId, GoalOwnerType.ORGANIZATION, orgId)
                .stream()
                .filter(goal -> cycle == null || goal.getCycle() == cycle)
                .map(GoalObjectiveResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoalViewResDto get(UUID goalId, UUID requesterId, UUID companyId, boolean hasEvalRead) {
        Goal goal = mustGet(goalId, companyId);
        if (!isVisible(goal, requesterId, companyId, hasEvalRead)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to read this goal.");
        }
        return GoalViewMapper.toView(goal);
    }

    @Transactional(readOnly = true)
    public GoalAggregateResDto getAggregate(UUID orgGoalId, UUID requesterId, UUID companyId, boolean hasEvalRead) {
        Goal orgGoal = mustGet(orgGoalId, companyId);
        if (orgGoal.getOwnerType() != GoalOwnerType.ORGANIZATION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Aggregate is available only for organization goals.");
        }
        if (!isVisible(orgGoal, requesterId, companyId, hasEvalRead)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to read this aggregate.");
        }

        List<Goal> children = goalRepository.findByAlignedOrgGoalId(orgGoalId).stream()
                .filter(g -> g.getOwnerType() == GoalOwnerType.MEMBER)
                .toList();

        Map<GoalStatus, Long> statusMap = children.stream()
                .collect(Collectors.groupingBy(Goal::getStatus, Collectors.counting()));

        BigDecimal weightedScoreSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal simpleScoreSum = BigDecimal.ZERO;
        int confirmedCount = 0;

        Map<String, List<EvaluationResponse>> responseCache = new HashMap<>();
        for (Goal child : children) {
            GoalScoreSnapshot scoreSnapshot = resolveFinalGoalScore(child, companyId, responseCache);
            if (scoreSnapshot == null) {
                continue;
            }
            confirmedCount++;
            simpleScoreSum = simpleScoreSum.add(scoreSnapshot.score());
            if (child.getWeightPct() > 0) {
                BigDecimal weight = BigDecimal.valueOf(child.getWeightPct());
                weightedScoreSum = weightedScoreSum.add(scoreSnapshot.score().multiply(weight));
                weightSum = weightSum.add(weight);
            }
        }

        BigDecimal weightedAvg = null;
        BigDecimal simpleAvg = null;
        if (confirmedCount > 0) {
            simpleAvg = simpleScoreSum.divide(BigDecimal.valueOf(confirmedCount), 2, RoundingMode.HALF_UP);
            if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
                weightedAvg = weightedScoreSum.divide(weightSum, 2, RoundingMode.HALF_UP);
            } else {
                weightedAvg = simpleAvg;
            }
        }

        return GoalAggregateResDto.builder()
                .orgGoalId(orgGoalId)
                .childCount(children.size())
                .confirmedCount(confirmedCount)
                .weightedAvgScore(weightedAvg)
                .simpleAvgScore(simpleAvg)
                .childGoalIds(children.stream().map(Goal::getGoalId).toList())
                .childCountByStatus(GoalAggregateResDto.emptyStatusMapWith(statusMap))
                .build();
    }

    private GoalScoreSnapshot resolveFinalGoalScore(Goal goal,
                                                    UUID companyId,
                                                    Map<String, List<EvaluationResponse>> responseCache) {
        String cacheKey = goal.getOwnerId() + ":" + goal.getCycle() + ":" + goal.getCycleStartDate();
        List<EvaluationResponse> matchingResponses = responseCache.computeIfAbsent(cacheKey, unused ->
                evaluationResponseRepository.findByCompanyIdAndTargetMemberIdOrderByCreatedAtDesc(companyId, goal.getOwnerId())
                        .stream()
                        .filter(r -> r.getStage() == EvaluationStage.CONFIRMED)
                        .filter(r -> r.getGroup() != null && r.getGroup().getSeason() != null)
                        .filter(r -> r.getGroup().getSeason().getTargetCycle() == goal.getCycle())
                        .filter(r -> goal.getCycleStartDate().equals(r.getGroup().getSeason().getTargetCycleStart()))
                        .toList()
        );

        for (EvaluationResponse response : matchingResponses) {
            EvaluationCalibration leadCalibration = evaluationCalibrationRepository
                    .findByResponseIdAndRole(response.getResponseId(), CalibrationRole.LEAD)
                    .orElse(null);
            if (leadCalibration == null || isBlank(leadCalibration.getFinalGradeJson())) {
                continue;
            }
            try {
                Map<String, String> grades = objectMapper.readValue(
                        leadCalibration.getFinalGradeJson(),
                        new TypeReference<Map<String, String>>() {}
                );
                String gradeValue = grades.get(goal.getGoalId().toString());
                if (isBlank(gradeValue)) {
                    continue;
                }
                Grade grade = Grade.valueOf(gradeValue.trim().toUpperCase());
                return new GoalScoreSnapshot(BigDecimal.valueOf(grade.getDefaultScore()));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isVisible(Goal goal, UUID requesterId, UUID companyId, boolean hasEvalRead) {
        UUID myOrgId = memberServiceClient.findOrganizationId(companyId, requesterId);
        if (goal.getOwnerType() == GoalOwnerType.MEMBER && goal.getOwnerId().equals(requesterId)) {
            return true;
        }
        if (goal.getOwnerType() == GoalOwnerType.ORGANIZATION && myOrgId != null && myOrgId.equals(goal.getOwnerId())) {
            return true;
        }
        if (goal.getParticipantMemberIds() != null && goal.getParticipantMemberIds().contains(requesterId)) {
            return true;
        }
        if (hasEvalRead) {
            return true;
        }
        return switch (goal.getVisibility()) {
            case COMPANY -> true;
            case TEAM -> {
                boolean matched = myOrgId != null
                        && goal.getVisibleTeamIds() != null
                        && goal.getVisibleTeamIds().contains(myOrgId);
                yield matched;
            }
            case PRIVATE -> false;
        };
    }

    private boolean isObjectiveAvailable(Goal goal, UUID requesterOrgId) {
        if (goal.getOwnerType() != GoalOwnerType.ORGANIZATION) {
            return false;
        }
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            return false;
        }
        if (goal.getVisibility() == GoalVisibility.COMPANY) {
            return true;
        }
        if (requesterOrgId != null && requesterOrgId.equals(goal.getOwnerId())) {
            return true;
        }
        if (goal.getVisibility() == GoalVisibility.TEAM && requesterOrgId != null && goal.getVisibleTeamIds() != null) {
            return goal.getVisibleTeamIds().contains(requesterOrgId);
        }
        return false;
    }

    private void validateOwner(GoalOwnerType ownerType, UUID ownerId, UUID requesterId, UUID companyId) {
        if (ownerType == GoalOwnerType.MEMBER) {
            if (!ownerId.equals(requesterId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "You can create KR only for yourself.");
            }
        }
    }

    private void ensureObjectiveOwner(Goal goal, UUID requesterId, UUID companyId, boolean canManageOrgGoal) {
        if (!canManageOrgGoal) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to manage organization objectives.");
        }
    }

    private void validateCycleDates(KpiCycle cycle, LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goal cycle dates are invalid.");
        }
        CycleKeyResolver.resolve(cycle, start);
    }

    private void validateAlignment(GoalOwnerType ownerType, UUID alignedOrgGoalId, UUID companyId) {
        if (ownerType == GoalOwnerType.ORGANIZATION && alignedOrgGoalId != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Objectives cannot align to another objective.");
        }
        if (ownerType == GoalOwnerType.MEMBER && alignedOrgGoalId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KR must be aligned to an objective.");
        }
        if (alignedOrgGoalId == null) {
            return;
        }
        Goal aligned = goalRepository.findById(alignedOrgGoalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective was not found."));
        if (!aligned.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective must belong to the same company.");
        }
        if (aligned.getOwnerType() != GoalOwnerType.ORGANIZATION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only organization goals can be used as alignment targets.");
        }
        if (aligned.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective must be active.");
        }
        validateGradeCriteria(
                aligned.getGradeSCriteria(),
                aligned.getGradeACriteria(),
                aligned.getGradeBCriteria(),
                aligned.getGradeCCriteria(),
                "Objective"
        );
    }

    private void validateGradeCriteria(String s, String a, String b, String c, String label) {
        if (isBlank(s) || isBlank(a) || isBlank(b) || isBlank(c)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "S/A/B/C grade criteria are required for " + label + ".");
        }
    }

    private void validateKrDefinition(Goal goal, UUID companyId) {
        if (goal.getAlignedOrgGoalId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KR must be aligned to an objective.");
        }
        Goal objective = goalRepository.findById(goal.getAlignedOrgGoalId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective was not found."));
        if (!objective.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective must belong to the same company.");
        }
        if (objective.getOwnerType() != GoalOwnerType.ORGANIZATION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Aligned objective must be an organization objective.");
        }
        validateGradeCriteria(
                objective.getGradeSCriteria(),
                objective.getGradeACriteria(),
                objective.getGradeBCriteria(),
                objective.getGradeCCriteria(),
                "Objective"
        );
    }

    private List<GoalKrResDto> mapKrDtos(List<Goal> goals, UUID companyId) {
        Map<UUID, Goal> objectiveMap = loadObjectiveMap(goals, companyId);
        return goals.stream()
                .map(goal -> GoalKrResDto.from(goal, objectiveMap.get(goal.getAlignedOrgGoalId())))
                .collect(Collectors.toList());
    }

    private Map<UUID, Goal> loadObjectiveMap(List<Goal> goals, UUID companyId) {
        Set<UUID> objectiveIds = goals.stream()
                .map(Goal::getAlignedOrgGoalId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (objectiveIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return goalRepository.findAllById(objectiveIds).stream()
                .filter(goal -> goal.getCompanyId().equals(companyId))
                .collect(Collectors.toMap(Goal::getGoalId, goal -> goal));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Goal mustGet(UUID goalId, UUID companyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Goal not found."));
        if (!goal.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Goal belongs to another company.");
        }
        return goal;
    }

    @Transactional(readOnly = true)
    public GoalViewResDto getForOwnerFlow(UUID goalId, UUID requesterId, UUID companyId) {
        return get(goalId, requesterId, companyId, false);
    }

    @Transactional
    public GoalViewResDto createForOwnerFlow(GoalCreateReqDto dto, UUID requesterId, UUID companyId) {
        return create(dto, requesterId, companyId);
    }

    @Transactional
    public GoalViewResDto activateForOwnerFlow(UUID goalId, UUID requesterId, UUID companyId) {
        Goal goal = mustGet(goalId, companyId);
        goal.activate(requesterId);
        return GoalViewMapper.toView(goal);
    }

    @Transactional(readOnly = true)
    public Goal requireReadable(UUID goalId, UUID requesterId, UUID companyId) {
        Goal goal = mustGet(goalId, companyId);
        if (!isVisible(goal, requesterId, companyId, false)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to read this goal.");
        }
        return goal;
    }

    @Transactional(readOnly = true)
    public boolean canPostManualProgress(Goal goal, UUID memberId) {
        if (goal == null || memberId == null) {
            return false;
        }
        if (memberId.equals(goal.getOwnerId())) {
            return true;
        }
        return goal.getParticipantMemberIds() != null && goal.getParticipantMemberIds().contains(memberId);
    }

    private record GoalScoreSnapshot(BigDecimal score) {
    }
}
