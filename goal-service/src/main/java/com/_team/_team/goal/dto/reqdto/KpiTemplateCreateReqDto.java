package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class KpiTemplateCreateReqDto {

    private UUID companyId;

    @NotBlank
    private String name;

    @NotNull
    private MeasureType measureType;

    @NotNull
    private UnitType unitType;

    @NotNull
    private KpiCycle cycle;

    @Min(0)
    @Max(200)
    private int capPct;
}
