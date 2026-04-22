package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResDto {

    private UUID goalId;
    private UUID kpiTemplateId;
    private UUID companyId;
    private UUID parentGoalId;
    private GoalOwnerType ownerType;
    private UUID ownerId;
    /**
     * TEAM 목표의 책임자 memberId. MEMBER 목표는 null (ownerId 자체가 책임자).
     * 프론트에서 완료 요청 가능 여부, 책임자 뱃지 표시 등에 사용.
     */
    private UUID responsibleMemberId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private MeasureType measureType;
    private UnitType unitType;
    private String unitLabel;
    private BigDecimal baseline;
    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private BigDecimal achievementPct;
    private BigDecimal rolledAchievementPct;
    /** 0~100 UI 진행률 — 롤업 값 우선 */
    private BigDecimal progress;
    private RollupSource rollupSource;
    private RollupPolicy rollupPolicy;
    private int capPct;
    private BigDecimal contributionPct;
    private BigDecimal weightPct;
    private GoalVisibility visibility;
    private GoalStatus status;
    private GoalApprovalStatus goalApprovalStatus;
    /** objective | kr | task */
    private String type;
    private GoalKind goalKind;
    private boolean autoUpdate;
    private GoalHealthStatus healthStatus;
    private List<UUID> visibleTeamIds;
    private List<UUID> participantMemberIds;
    private Integer childCount;
    private Boolean hasChildren;
    private Integer depth;
    private List<UUID> path;
    private KpiCycle cycle;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    /** 목표 생성 시 지정된 "종료 승인자". 완료 요청 모달에서 기본값으로 사용된다. */
    private UUID completionApproverId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GoalResDto from(Goal goal) {
        BigDecimal rolled = goal.getRolledAchievementPct();
        BigDecimal ach = goal.getAchievementPct();
        BigDecimal progressVal = rolled != null ? rolled : (ach != null ? ach : BigDecimal.ZERO);
        return GoalResDto.builder()
                .goalId(goal.getGoalId())
                .kpiTemplateId(goal.getKpiTemplate() != null ? goal.getKpiTemplate().getKpiTemplateId() : null)
                .companyId(goal.getCompanyId())
                .parentGoalId(goal.getParentGoal() != null ? goal.getParentGoal().getGoalId() : null)
                .ownerType(goal.getOwnerType())
                .ownerId(goal.getOwnerId())
                .responsibleMemberId(goal.getResponsibleMemberId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .measureType(goal.getMeasureType())
                .unitType(goal.getUnitType())
                .unitLabel(goal.getUnitLabel())
                .baseline(goal.getBaseline())
                .targetValue(goal.getTargetValue())
                .actualValue(goal.getActualValue())
                .achievementPct(goal.getAchievementPct())
                .rolledAchievementPct(goal.getRolledAchievementPct())
                .progress(progressVal)
                .rollupSource(goal.getRollupSource())
                .rollupPolicy(goal.getRollupPolicy())
                .capPct(goal.getCapPct())
                .contributionPct(goal.getContributionPct())
                .weightPct(goal.getWeightPct())
                .visibility(goal.getVisibility())
                .status(goal.getStatus())
                .goalApprovalStatus(goal.getGoalApprovalStatus())
                .type(goal.getGoalKind() != null ? goal.getGoalKind().name().toLowerCase() : "kr")
                .goalKind(goal.getGoalKind())
                .autoUpdate(goal.isAutoUpdate())
                .healthStatus(goal.getHealthStatus())
                .visibleTeamIds(goal.getVisibleTeamIds() != null ? goal.getVisibleTeamIds() : Collections.emptyList())
                .participantMemberIds(goal.getParticipantMemberIds() != null ? goal.getParticipantMemberIds() : Collections.emptyList())
                .childCount(0)
                .hasChildren(false)
                .depth(0)
                .path(List.of(goal.getGoalId()))
                .cycle(goal.getCycle() != null ? goal.getCycle() : (goal.getKpiTemplate() != null ? goal.getKpiTemplate().getCycle() : null))
                .approvedBy(goal.getApprovedBy())
                .approvedAt(goal.getApprovedAt())
                .completionApproverId(goal.getCompletionApproverId())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
