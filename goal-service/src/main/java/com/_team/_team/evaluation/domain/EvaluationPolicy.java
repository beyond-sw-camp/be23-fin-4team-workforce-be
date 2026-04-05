package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.EvalCycle;
import com._team._team.evaluation.domain.enums.GradeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evaluation_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evaluation_policy_id", columnDefinition = "BINARY(16)")
    private UUID evaluationPolicyId;

    @Column(name = "company_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID companyId;

    @Column(name = "policy_name", length = 100, nullable = false)
    private String policyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "eval_cycle", nullable = false)
    private EvalCycle evalCycle;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "result_open_date")
    private LocalDate resultOpenDate;

    @Column(name = "edit_allowed_days", nullable = false)
    private int editAllowedDays;

    @Column(name = "quant_weight_pct", nullable = false)
    private int quantWeightPct;

    @Column(name = "qual_weight_pct", nullable = false)
    private int qualWeightPct;

    @Column(name = "self_weight_pct", nullable = false)
    private int selfWeightPct;

    @Column(name = "supervisor_weight_pct", nullable = false)
    private int supervisorWeightPct;

    @Column(name = "peer_weight_pct", nullable = false)
    private int peerWeightPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false)
    private GradeType gradeType;

    @Column(name = "grade_config_json", columnDefinition = "JSON")
    private String gradeConfigJson;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "bias_check_enabled", nullable = false)
    private boolean biasCheckEnabled;

    @Column(name = "peer_count_min", nullable = false)
    private int peerCountMin;

    @Column(name = "peer_count_max", nullable = false)
    private int peerCountMax;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public void deactivate() {
        this.isActive = false;
    }
}
