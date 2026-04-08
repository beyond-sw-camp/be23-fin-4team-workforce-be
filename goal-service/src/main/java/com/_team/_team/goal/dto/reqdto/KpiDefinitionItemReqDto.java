package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class KpiDefinitionItemReqDto {
    @NotBlank
    private String title;
    @NotNull
    private BigDecimal targetValue;
    @NotBlank
    private String unit;
    @NotNull
    private UUID ownerId;
    private GoalOwnerType ownerType;
}
