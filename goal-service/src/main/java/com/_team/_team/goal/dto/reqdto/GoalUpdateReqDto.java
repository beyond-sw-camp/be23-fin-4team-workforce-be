package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
