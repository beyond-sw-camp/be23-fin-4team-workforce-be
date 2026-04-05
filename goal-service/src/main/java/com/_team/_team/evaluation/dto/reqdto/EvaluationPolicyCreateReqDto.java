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

    @NotNull
    private UUID companyId;

    @NotBlank
    @Size(max = 100)
    private String policyName;

    @NotNull
    private EvalCycle evalCycle;

    @NotNull
    private LocalDate periodStart;

    @NotNull
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

    @NotNull
    private GradeType gradeType;

    private String gradeConfigJson;

    private boolean approvalRequired;

    private boolean biasCheckEnabled;

    @Min(0)
    private int peerCountMin;

    @Min(1)
    private int peerCountMax;
}
