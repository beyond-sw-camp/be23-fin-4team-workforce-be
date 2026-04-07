package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiTemplateCreateReqDto {

    private UUID companyId;

    @NotBlank(message = "KPI 템플릿명은 필수입니다.")
    private String name;

    @NotNull(message = "측정 유형은 필수입니다.")
    private MeasureType measureType;

    @NotNull(message = "단위 유형은 필수입니다.")
    private UnitType unitType;

    @NotBlank(message = "단위 표시명은 필수입니다.")
    @Size(max = 20, message = "단위 표시명은 20자 이하여야 합니다.")
    private String unitLabel;

    @NotNull(message = "측정 주기는 필수입니다.")
    private KpiCycle cycle;

    @Min(0)
    @Max(200)
    private int capPct;
}
