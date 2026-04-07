package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.Evaluation;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResDto {

    private UUID evaluationId;
    private UUID evaluationPolicyId;
    private UUID evaluatorId;
    private UUID evaluateeId;
    private EvalType evalType;
    private String goalScoresJson;
    private BigDecimal quantScore;
    private BigDecimal qualScore;
    private String rubricScoresJson;
    private String comment;
    private String strengthComment;
    private String improveComment;
    private BigDecimal finalScore;
    private String grade;
    private boolean biasChecked;
    private String biasType;
    private EvaluationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EvaluationResDto from(Evaluation evaluation) {
        return EvaluationResDto.builder()
                .evaluationId(evaluation.getEvaluationId())
                .evaluationPolicyId(evaluation.getEvaluationPolicy().getEvaluationPolicyId())
                .evaluatorId(evaluation.getEvaluatorId())
                .evaluateeId(evaluation.getEvaluateeId())
                .evalType(evaluation.getEvalType())
                .goalScoresJson(evaluation.getGoalScoresJson())
                .quantScore(evaluation.getQuantScore())
                .qualScore(evaluation.getQualScore())
                .rubricScoresJson(evaluation.getRubricScoresJson())
                .comment(evaluation.getComment())
                .strengthComment(evaluation.getStrengthComment())
                .improveComment(evaluation.getImproveComment())
                .finalScore(evaluation.getFinalScore())
                .grade(evaluation.getGrade())
                .biasChecked(evaluation.isBiasChecked())
                .biasType(evaluation.getBiasType())
                .status(evaluation.getStatus())
                .submittedAt(evaluation.getSubmittedAt())
                .createdAt(evaluation.getCreatedAt())
                .updatedAt(evaluation.getUpdatedAt())
                .build();
    }
}
