package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalHealthStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoalProgressUpdateReqDto {
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal value;

    @NotNull
    private GoalHealthStatus status;

    private String note;
}
