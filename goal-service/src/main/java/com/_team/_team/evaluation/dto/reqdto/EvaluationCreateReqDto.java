package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.enums.EvalType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class EvaluationCreateReqDto {

    @NotNull
    private UUID evaluationPolicyId;

    @NotNull
    private UUID evaluatorId;

    @NotNull
    private UUID evaluateeId;

    @NotNull
    private EvalType evalType;
}
