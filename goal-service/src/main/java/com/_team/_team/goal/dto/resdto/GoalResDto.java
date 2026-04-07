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
    private int capPct;
    private BigDecimal contributionPct;
    private BigDecimal weightPct;
    private GoalVisibility visibility;
    private GoalStatus status;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GoalResDto from(Goal goal) {
        return GoalResDto.builder()
                .goalId(goal.getGoalId())
                .kpiTemplateId(goal.getKpiTemplate() != null ? goal.getKpiTemplate().getKpiTemplateId() : null)
                .companyId(goal.getCompanyId())
                .parentGoalId(goal.getParentGoal() != null ? goal.getParentGoal().getGoalId() : null)
                .ownerType(goal.getOwnerType())
                .ownerId(goal.getOwnerId())
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
                .capPct(goal.getCapPct())
                .contributionPct(goal.getContributionPct())
                .weightPct(goal.getWeightPct())
                .visibility(goal.getVisibility())
                .status(goal.getStatus())
                .approvedBy(goal.getApprovedBy())
                .approvedAt(goal.getApprovedAt())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
