package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Evaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evaluation_id")
    private UUID evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_policy_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private EvaluationPolicy evaluationPolicy;

    @Column(name = "evaluator_id", nullable = false)
    private UUID evaluatorId;

    @Column(name = "evaluatee_id", nullable = false)
    private UUID evaluateeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "eval_type", nullable = false)
    private EvalType evalType;

    @Column(name = "goal_scores_json", columnDefinition = "JSON")
    private String goalScoresJson;

    @Column(name = "quant_score", precision = 7, scale = 2)
    private BigDecimal quantScore;

    @Column(name = "qual_score", precision = 7, scale = 2)
    private BigDecimal qualScore;

    @Column(name = "rubric_scores_json", columnDefinition = "JSON")
    private String rubricScoresJson;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "strength_comment", columnDefinition = "TEXT")
    private String strengthComment;

    @Column(name = "improve_comment", columnDefinition = "TEXT")
    private String improveComment;

    @Column(name = "final_score", precision = 7, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "grade", length = 5)
    private String grade;

    @Builder.Default
    @Column(name = "bias_checked", nullable = false)
    private boolean biasChecked = false;

    @Column(name = "bias_type", length = 50)
    private String biasType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EvaluationStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public void submit() {
        this.status = EvaluationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void confirm(BigDecimal finalScore, String grade) {
        this.status = EvaluationStatus.CONFIRMED;
        this.finalScore = finalScore;
        this.grade = grade;
    }

    public void updateScores(BigDecimal quantScore, BigDecimal qualScore,
                             String goalScoresJson, String rubricScoresJson) {
        this.quantScore = quantScore;
        this.qualScore = qualScore;
        this.goalScoresJson = goalScoresJson;
        this.rubricScoresJson = rubricScoresJson;
    }

    public void flagBias(String biasType) {
        this.biasChecked = true;
        this.biasType = biasType;
    }
}
