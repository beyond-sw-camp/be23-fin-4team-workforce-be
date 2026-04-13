package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_response")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "response_id")
    private UUID responseId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private EvaluationGroup group;

    @Column(name = "target_member_id", nullable = false)
    private UUID targetMemberId;

    @Column(name = "evaluator_id", nullable = false)
    private UUID evaluatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false)
    private EvalType evaluationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.NOT_STARTED;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_reminded_at")
    private LocalDateTime lastRemindedAt;

    @Column(name = "answers_json", columnDefinition = "JSON")
    private String answersJson;

    @Column(name = "calibration_json", columnDefinition = "JSON")
    private String calibrationJson;

    public void startProgress() {
        if (this.status == EvaluationStatus.NOT_STARTED) {
            this.status = EvaluationStatus.IN_PROGRESS;
        }
    }

    public void saveAnswers(String answersJson) {
        this.answersJson = answersJson;
        if (this.status == EvaluationStatus.NOT_STARTED) {
            this.status = EvaluationStatus.IN_PROGRESS;
        }
    }

    public void submit() {
        this.status = EvaluationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = EvaluationStatus.IN_PROGRESS;
        this.submittedAt = null;
    }

    public void updateRemindedAt() {
        this.lastRemindedAt = LocalDateTime.now();
    }

    public void updateCalibration(String calibrationJson) {
        this.calibrationJson = calibrationJson;
    }
}
