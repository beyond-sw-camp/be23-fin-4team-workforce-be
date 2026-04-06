package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "kpi_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KpiTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kpi_template_id", columnDefinition = "BINARY(16)")
    private UUID kpiTemplateId;

    @Column(name = "company_id", columnDefinition = "BINARY(16)", nullable = true)
    private UUID companyId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_type", nullable = false)
    private MeasureType measureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", nullable = false)
    private KpiCycle cycle;

    @Column(name = "cap_pct", nullable = false)
    private int capPct;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public void deactivate() {
        this.isActive = false;
    }
}
