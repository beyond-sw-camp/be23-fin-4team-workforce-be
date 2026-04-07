package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class GoalCreateReqDto {

    @NotNull
    private UUID kpiTemplateId;

    @NotNull
    private UUID companyId;

    private UUID parentGoalId;

    @NotNull
    private GoalOwnerType ownerType;

    @NotNull
    private UUID ownerId;

    @NotBlank
    @Size(max = 300)
    private String title;

    private String description;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private MeasureType measureType;

    @NotNull
    private UnitType unitType;

    private String unitLabel;

    @NotNull
    private BigDecimal baseline;

    @NotNull
    private BigDecimal targetValue;

    private int capPct;

    private BigDecimal contributionPct;

    private BigDecimal weightPct;

    @NotNull
    private GoalVisibility visibility;
}
