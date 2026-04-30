package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.GoalActivity;
import com._team._team.goal.domain.enums.GoalActivityType;
import com._team._team.goal.domain.enums.GoalHealthStatus;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.dto.reqdto.GoalProgressUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalProgressUpdateResDto;
import com._team._team.goal.repository.GoalActivityRepository;
import com._team._team.goal.repository.GoalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GoalProgressUpdateService {

    private final GoalRepository goalRepository;
    private final GoalActivityRepository goalActivityRepository;
    private final GoalRollupService goalRollupService;
    private final GoalActivityService goalActivityService;
    private final GoalService goalService;
    private final ObjectMapper objectMapper;

    public GoalProgressUpdateService(
            GoalRepository goalRepository,
            GoalActivityRepository goalActivityRepository,
            GoalRollupService goalRollupService,
            GoalActivityService goalActivityService,
            GoalService goalService,
            ObjectMapper objectMapper) {
        this.goalRepository = goalRepository;
        this.goalActivityRepository = goalActivityRepository;
        this.goalRollupService = goalRollupService;
        this.goalActivityService = goalActivityService;
        this.goalService = goalService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GoalProgressUpdateResDto addUpdate(
            UUID goalId,
            GoalProgressUpdateReqDto dto,
            UUID memberId,
            UUID companyId,
            boolean hasGoalUpdatePermission) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표를 찾을 수 없습니다."));
        if (!goal.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 목표입니다.");
        }
        if (!goalService.canPostManualProgress(goal, memberId) && !hasGoalUpdatePermission) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "담당자·참여 멤버이거나 목표 수정(GOAL:UPDATE) 권한이 있어야 진행률을 업데이트할 수 있습니다.");
        }
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 목표만 업데이트할 수 있습니다.");
        }
        if (goal.isAutoUpdate()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "자동 집계(⚡) 목표는 수동 진행률을 입력할 수 없습니다.");
        }
        goal.applyManualProgress(dto.getValue(), dto.getStatus());
        goalRollupService.recalculateFrom(goalId);
        String summary = String.format("진행률을 업데이트했어요. 수치: %s%%, 상태: %s",
                dto.getValue().stripTrailingZeros().toPlainString(),
                healthLabelKo(dto.getStatus()));
        goalActivityService.append(goalId, GoalActivityType.PROGRESS_UPDATED, memberId, summary, buildProgressMeta(dto));
        return GoalProgressUpdateResDto.builder()
                .updateId(null)
                .goalId(goalId)
                .value(dto.getValue())
                .status(dto.getStatus())
                .note(dto.getNote())
                .createdBy(memberId)
                .createdAt(null)
                .build();
    }

    private static String healthLabelKo(GoalHealthStatus s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case NOT_STARTED -> "미착수";
            case ON_TRACK -> "순조로움";
            case AT_RISK -> "주의";
            case BEHIND -> "지연";
            case COMPLETED -> "완료";
        };
    }

    private String buildProgressMeta(GoalProgressUpdateReqDto dto) {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("progressPct", dto.getValue().stripTrailingZeros().doubleValue());
            n.put("healthStatus", dto.getStatus().name());
            if (dto.getNote() != null && !dto.getNote().isBlank()) {
                n.put("note", dto.getNote());
            }
            return objectMapper.writeValueAsString(n);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<GoalProgressUpdateResDto> list(UUID goalId, UUID requesterId, UUID companyId) {
        goalService.requireReadable(goalId, requesterId, companyId);
        return goalActivityRepository.findByGoal_GoalIdAndTypeOrderByCreatedAtDesc(goalId, GoalActivityType.PROGRESS_UPDATED).stream()
                .map(this::toProgressUpdateRes)
                .toList();
    }

    private GoalProgressUpdateResDto toProgressUpdateRes(GoalActivity activity) {
        ObjectNode meta = null;
        try {
            if (activity.getMetaJson() != null && !activity.getMetaJson().isBlank()) {
                meta = (ObjectNode) objectMapper.readTree(activity.getMetaJson());
            }
        } catch (Exception ignored) {
            meta = null;
        }
        return GoalProgressUpdateResDto.builder()
                .updateId(activity.getActivityId())
                .goalId(activity.getGoal().getGoalId())
                .value(meta != null && meta.has("progressPct") ? meta.get("progressPct").decimalValue() : null)
                .status(meta != null && meta.has("healthStatus")
                        ? GoalHealthStatus.valueOf(meta.get("healthStatus").asText())
                        : null)
                .note(meta != null && meta.has("note") ? meta.get("note").asText() : null)
                .createdBy(activity.getActorId())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
