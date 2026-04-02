package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalProgressStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalCheckinReqDto {
    private BigDecimal actualValue;
    @NotNull
    private GoalProgressStatus progressStatus;
    private String comment;
}
