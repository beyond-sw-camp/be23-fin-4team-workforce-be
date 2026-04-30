package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalApprovalStatus;
import com._team._team.goal.domain.enums.GoalOwnerType;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.util.CycleKeyResolver;
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
public class GoalObjectiveResDto implements GoalViewResDto {
    private UUID goalId;
    private UUID companyId;
    private GoalOwnerType ownerType;
    private UUID ownerId;
    private String title;
    private String description;
    private KpiCycle cycle;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private String cycleKey;
    private GoalVisibility visibility;
    private GoalStatus status;
    private GoalApprovalStatus goalApprovalStatus;
    private List<UUID> visibleTeamIds;
    private List<UUID> participantMemberIds;
    private String gradeS;
    private String gradeA;
    private String gradeB;
    private String gradeC;
    private BigDecimal actualValue;
    private BigDecimal achievementPct;
    private BigDecimal rolledAchievementPct;
    private String healthStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GoalObjectiveResDto from(Goal goal) {
        return GoalObjectiveResDto.builder()
                .goalId(goal.getGoalId())
                .companyId(goal.getCompanyId())
                .ownerType(goal.getOwnerType())
                .ownerId(goal.getOwnerId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .cycle(goal.getCycle())
                .cycleStartDate(goal.getCycleStartDate())
                .cycleEndDate(goal.getCycleEndDate())
                .cycleKey(CycleKeyResolver.resolve(goal.getCycle(), goal.getCycleStartDate()))
                .visibility(goal.getVisibility())
                .status(goal.getStatus())
                .goalApprovalStatus(goal.getGoalApprovalStatus())
                .visibleTeamIds(goal.getVisibleTeamIds() != null ? goal.getVisibleTeamIds() : Collections.emptyList())
                .participantMemberIds(goal.getParticipantMemberIds() != null ? goal.getParticipantMemberIds() : Collections.emptyList())
                .gradeS(goal.getGradeSCriteria())
                .gradeA(goal.getGradeACriteria())
                .gradeB(goal.getGradeBCriteria())
                .gradeC(goal.getGradeCCriteria())
                .actualValue(goal.getActualValue())
                .achievementPct(goal.getAchievementPct())
                .rolledAchievementPct(goal.getRolledAchievementPct())
                .healthStatus(goal.getHealthStatus() == null ? null : goal.getHealthStatus().name())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
