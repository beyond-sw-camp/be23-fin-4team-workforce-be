package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiTemplateResDto {

    private UUID kpiTemplateId;
    private UUID companyId;
    private String name;
    private MeasureType measureType;
    private UnitType unitType;
    private String unitLabel;
    private KpiCycle cycle;
    private int capPct;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KpiTemplateResDto from(KpiTemplate kpiTemplate) {
        return KpiTemplateResDto.builder()
                .kpiTemplateId(kpiTemplate.getKpiTemplateId())
                .companyId(kpiTemplate.getCompanyId())
                .name(kpiTemplate.getName())
                .measureType(kpiTemplate.getMeasureType())
                .unitType(kpiTemplate.getUnitType())
                .unitLabel(kpiTemplate.getUnitLabel())
                .cycle(kpiTemplate.getCycle())
                .capPct(kpiTemplate.getCapPct())
                .isActive(kpiTemplate.isActive())
                .createdAt(kpiTemplate.getCreatedAt())
                .updatedAt(kpiTemplate.getUpdatedAt())
                .build();
    }
}
