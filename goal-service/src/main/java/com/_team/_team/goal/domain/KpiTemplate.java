package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.converter.KpiDefinition;
import com._team._team.goal.domain.converter.KpiDefinitionListConverter;
import com._team._team.goal.domain.enums.GoalApprovalPolicy;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.KpiSpecCycleType;
import com._team._team.goal.domain.enums.MeasureType;
import com._team._team.goal.domain.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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
    @Column(name = "kpi_template_id")
    private UUID kpiTemplateId;

    @Column(name = "company_id", nullable = true)
    private UUID companyId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_type", nullable = false)
    private MeasureType measureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    /** 화면 표시용 단위명 (예: %, 건, 시간) */
    @Column(name = "unit_label", length = 20, nullable = false)
    private String unitLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", nullable = false)
    private KpiCycle cycle;

    @Column(name = "cap_pct", nullable = false)
    private int capPct;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "spec_cycle_type", length = 32)
    private KpiSpecCycleType specCycleType;

    @Column(name = "target_team_id")
    private UUID targetTeamId;

    /**
     * 승인 정책 (NONE / ACTIVATION_ONLY / COMPLETION_ONLY / BOTH).
     * [D-1] 기존 requireApproval 불리언은 제거. 이 필드만 진실 공급원.
     * 기존 DB 컬럼 `require_approval` 은 이후 마이그레이션에서 DROP.
     *   - ALTER TABLE kpi_template DROP COLUMN require_approval;
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_approval_policy", length = 32, nullable = false)
    @Builder.Default
    private GoalApprovalPolicy goalApprovalPolicy = GoalApprovalPolicy.NONE;

    /** KPI 정의 목록 — DB에는 JSON 배열로 저장 */
    @Convert(converter = KpiDefinitionListConverter.class)
    @Column(name = "kpis_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<KpiDefinition> kpis = new ArrayList<>();

    public void deactivate() {
        this.isActive = false;
    }

    public void setKpis(List<KpiDefinition> kpis) {
        this.kpis = kpis != null ? new ArrayList<>(kpis) : new ArrayList<>();
    }

    /**
     * 스펙 메타(주기·대상팀·승인정책) 변경. GoalApprovalPolicy 하나로 단일화.
     */
    public void setSpecMeta(KpiSpecCycleType specCycleType, UUID targetTeamId, GoalApprovalPolicy approvalPolicy) {
        if (specCycleType != null) this.specCycleType = specCycleType;
        if (targetTeamId != null) this.targetTeamId = targetTeamId;
        if (approvalPolicy != null) this.goalApprovalPolicy = approvalPolicy;
    }

    public void setGoalApprovalPolicy(GoalApprovalPolicy policy) {
        if (policy != null) this.goalApprovalPolicy = policy;
    }

    /** 실제 적용될 정책 반환. null 방어를 위해 NONE 을 기본값으로 매핑. */
    public GoalApprovalPolicy resolveApprovalPolicy() {
        return this.goalApprovalPolicy != null ? this.goalApprovalPolicy : GoalApprovalPolicy.NONE;
    }
}