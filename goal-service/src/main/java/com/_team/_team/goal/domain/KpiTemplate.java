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

    @Column(name = "require_approval", nullable = false)
    @Builder.Default
    private boolean requireApproval = false;

    /**
     * 세분화된 승인 정책 (NONE / ACTIVATION_ONLY / COMPLETION_ONLY / BOTH).
     *
     * 기존 requireApproval 불리언을 확장한 개념이며 아래 우선순위로 해석한다:
     *  1. goalApprovalPolicy != null  → 그 값을 그대로 사용
     *  2. goalApprovalPolicy == null  → requireApproval 을 BOTH/NONE 로 매핑 (레거시 호환)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_approval_policy", length = 32)
    private GoalApprovalPolicy goalApprovalPolicy;

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

    public void setSpecMeta(KpiSpecCycleType specCycleType, UUID targetTeamId, boolean requireApproval) {
        if (specCycleType != null) this.specCycleType = specCycleType;
        if (targetTeamId != null) this.targetTeamId = targetTeamId;
        this.requireApproval = requireApproval;
    }

    /**
     * 세분화 승인 정책 설정. 호출 시 레거시 requireApproval 플래그도 동기화하여
     * 기존 요구 사항(불리언 기반) 로직이 계속 동작하도록 한다.
     */
    public void setGoalApprovalPolicy(GoalApprovalPolicy policy) {
        this.goalApprovalPolicy = policy;
        if (policy != null) {
            this.requireApproval = policy != GoalApprovalPolicy.NONE;
        }
    }

    /**
     * 실제 적용될 정책을 반환. goalApprovalPolicy 가 null 이면 requireApproval 로부터 유도.
     */
    public GoalApprovalPolicy resolveApprovalPolicy() {
        if (this.goalApprovalPolicy != null) return this.goalApprovalPolicy;
        return GoalApprovalPolicy.fromLegacyFlag(this.requireApproval);
    }
}
