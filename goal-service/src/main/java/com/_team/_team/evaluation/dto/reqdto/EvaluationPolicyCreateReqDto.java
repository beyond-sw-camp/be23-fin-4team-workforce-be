package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.enums.EvalCycle;
import com._team._team.evaluation.domain.enums.GradeType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class EvaluationPolicyCreateReqDto {

    @NotNull(message = "회사 ID는 필수입니다.")
    private UUID companyId;

    @NotBlank(message = "정책명은 필수입니다.")
    @Size(max = 100)
    private String policyName;

    @NotNull(message = "평가 주기는 필수입니다.")
    private EvalCycle evalCycle;

    @NotNull(message = "평가 기간 시작일은 필수입니다.")
    private LocalDate periodStart;

    @NotNull(message = "평가 기간 종료일은 필수입니다.")
    private LocalDate periodEnd;

    private LocalDate resultOpenDate;

    @Min(0)
    private int editAllowedDays;

    @Min(0) @Max(100)
    private int quantWeightPct;

    @Min(0) @Max(100)
    private int qualWeightPct;

    @Min(0) @Max(100)
    private int selfWeightPct;

    @Min(0) @Max(100)
    private int supervisorWeightPct;

    @Min(0) @Max(100)
    private int peerWeightPct;

    @NotNull(message = "등급 유형은 필수입니다.")
    private GradeType gradeType;

    private String gradeConfigJson;

    private boolean approvalRequired;

    private boolean biasCheckEnabled;

    @Min(0)
    private int peerCountMin;

    @Min(1)
    private int peerCountMax;
}
