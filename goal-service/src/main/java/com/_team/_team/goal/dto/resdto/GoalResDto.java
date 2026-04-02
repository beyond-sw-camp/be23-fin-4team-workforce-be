package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalProgressStatus;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResDto {
    private UUID goalId;
    private UUID parentGoalId;
    private UUID cycleId;
    private UUID ownerId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal baseline;
    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private BigDecimal achievementPct;
    private Boolean isAutoUpdated;
    private GoalProgressStatus progressStatus;
    private GoalStatus goalStatus;
    private GoalVisibility visibility;

    public static GoalResDto fromEntity(Goal goal) {
        return GoalResDto.builder()
                .goalId(goal.getGoalId())
                .parentGoalId(goal.getParentGoal() != null ? goal.getParentGoal().getGoalId() : null)
                .cycleId(goal.getCycle() != null ? goal.getCycle().getCycleId() : null)
                .ownerId(goal.getOwnerId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .baseline(goal.getBaseline())
                .targetValue(goal.getTargetValue())
                .actualValue(goal.getActualValue())
                .achievementPct(goal.getAchievementPct())
                .isAutoUpdated(goal.getIsAutoUpdated())
                .progressStatus(goal.getProgressStatus())
                .goalStatus(goal.getGoalStatus())
                .visibility(goal.getVisibility())
                .build();
    }
}
