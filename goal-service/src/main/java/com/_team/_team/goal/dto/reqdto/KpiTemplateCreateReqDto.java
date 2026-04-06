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

    @NotBlank(message = "KPI 템플릿명은 필수입니다.")
    private String name;

    @NotNull(message = "측정 유형은 필수입니다.")
    private MeasureType measureType;

    @NotNull(message = "단위 유형은 필수입니다.")
    private UnitType unitType;

    @NotNull(message = "측정 주기는 필수입니다.")
    private KpiCycle cycle;

    @Min(0)
    @Max(200)
    private int capPct;
}
