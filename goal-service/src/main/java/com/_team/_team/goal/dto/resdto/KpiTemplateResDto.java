package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.converter.KpiDefinition;
import com._team._team.goal.domain.enums.GoalApprovalPolicy;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.KpiSpecCycleType;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    private KpiSpecCycleType specCycleType;
    private UUID targetTeamId;
    /**
     * 레거시 UI 호환 — goalApprovalPolicy 로부터 파생값.
     * NONE 아니면 true.
     */
    private boolean requireApproval;
    /** 세분화된 승인 정책. NONE/ACTIVATION_ONLY/COMPLETION_ONLY/BOTH. */
    private GoalApprovalPolicy goalApprovalPolicy;
    private List<KpiDefinition> kpis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KpiTemplateResDto from(KpiTemplate kpiTemplate) {
        GoalApprovalPolicy policy = kpiTemplate.resolveApprovalPolicy();
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
                .specCycleType(kpiTemplate.getSpecCycleType())
                .targetTeamId(kpiTemplate.getTargetTeamId())
                .requireApproval(policy != GoalApprovalPolicy.NONE)
                .goalApprovalPolicy(policy)
                .kpis(kpiTemplate.getKpis() != null ? kpiTemplate.getKpis() : Collections.emptyList())
                .createdAt(kpiTemplate.getCreatedAt())
                .updatedAt(kpiTemplate.getUpdatedAt())
                .build();
    }
}
