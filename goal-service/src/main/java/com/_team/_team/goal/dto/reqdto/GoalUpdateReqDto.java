package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class GoalUpdateReqDto {

    private String title;

    private String description;

    private GoalVisibility visibility;

    private BigDecimal weightPct;

    private BigDecimal contributionPct;
}
