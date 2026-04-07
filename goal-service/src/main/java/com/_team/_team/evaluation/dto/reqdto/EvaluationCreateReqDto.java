package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.enums.EvalType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCreateReqDto {

    @NotNull(message = "평가 정책 ID는 필수입니다.")
    private UUID evaluationPolicyId;

    @NotNull(message = "평가자 ID는 필수입니다.")
    private UUID evaluatorId;

    @NotNull(message = "피평가자 ID는 필수입니다.")
    private UUID evaluateeId;

    @NotNull(message = "평가 유형은 필수입니다.")
    private EvalType evalType;
}
