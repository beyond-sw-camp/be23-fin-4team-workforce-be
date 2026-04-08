package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalHealthStatus;
import com._team._team.goal.domain.enums.GoalKind;
import com._team._team.goal.domain.enums.GoalVisibility;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.RollupPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalUpdateReqDto {

    private String title;
    private String description;
    private GoalVisibility visibility;
    private BigDecimal weightPct;
    private BigDecimal contributionPct;
    private UUID parentGoalId;
    private KpiCycle cycle;
    private RollupPolicy rollupPolicy;
    private GoalKind goalKind;
    private Boolean autoUpdate;
    private GoalHealthStatus healthStatus;
    private List<UUID> visibleTeamIds;
    private List<UUID> memberIds;
}
