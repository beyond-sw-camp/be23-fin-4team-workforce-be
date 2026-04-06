package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationPolicy;
import com._team._team.evaluation.domain.enums.EvalCycle;
import com._team._team.evaluation.domain.enums.GradeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationPolicyResDto {

    private UUID evaluationPolicyId;
    private UUID companyId;
    private String policyName;
    private EvalCycle evalCycle;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate resultOpenDate;
    private int editAllowedDays;
    private int quantWeightPct;
    private int qualWeightPct;
    private int selfWeightPct;
    private int supervisorWeightPct;
    private int peerWeightPct;
    private GradeType gradeType;
    private String gradeConfigJson;
    private boolean approvalRequired;
    private boolean biasCheckEnabled;
    private int peerCountMin;
    private int peerCountMax;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EvaluationPolicyResDto from(EvaluationPolicy policy) {
        return EvaluationPolicyResDto.builder()
                .evaluationPolicyId(policy.getEvaluationPolicyId())
                .companyId(policy.getCompanyId())
                .policyName(policy.getPolicyName())
                .evalCycle(policy.getEvalCycle())
                .periodStart(policy.getPeriodStart())
                .periodEnd(policy.getPeriodEnd())
                .resultOpenDate(policy.getResultOpenDate())
                .editAllowedDays(policy.getEditAllowedDays())
                .quantWeightPct(policy.getQuantWeightPct())
                .qualWeightPct(policy.getQualWeightPct())
                .selfWeightPct(policy.getSelfWeightPct())
                .supervisorWeightPct(policy.getSupervisorWeightPct())
                .peerWeightPct(policy.getPeerWeightPct())
                .gradeType(policy.getGradeType())
                .gradeConfigJson(policy.getGradeConfigJson())
                .approvalRequired(policy.isApprovalRequired())
                .biasCheckEnabled(policy.isBiasCheckEnabled())
                .peerCountMin(policy.getPeerCountMin())
                .peerCountMax(policy.getPeerCountMax())
                .isActive(policy.isActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
