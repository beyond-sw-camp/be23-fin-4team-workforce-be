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

    /**
     * 이 평가 응답이 평가 대상으로 삼은 goal id 목록 (JSON 배열).
     * goal-service 모듈 내 Goal 엔티티에 대한 참조를 외래키 대신 ID 목록으로 유지하여
     * 결재/캘리브레이션 과정에서 실제 성과와의 링크를 추적할 수 있게 합니다.
     */
    @Column(name = "target_goal_ids_json", columnDefinition = "TEXT")
    private String targetGoalIdsJson;

    /**
     * 응답 채점으로 산출된 정규화된 점수 (0~100).
     * answersJson 만 있고 점수가 없어서 집계가 불가능했던 문제를 해결합니다.
     */
    @Column(name = "normalized_score", precision = 7, scale = 2)
    private java.math.BigDecimal normalizedScore;

    /**
     * 평가 시즌 시작 시점(혹은 응답 materialize 시점)에 캡처된 타깃 목표들의 스냅샷.
     * 목표가 나중에 수정·종료되더라도 "그 때 그 목표"를 기준으로 평가 맥락을 재현하기 위한
     * 읽기 전용 스냅샷이며, {@code List<GoalSnapshotDto>} 를 직렬화한 JSON 배열이다.
     *
     * 별도 엔티티를 추가하지 않고 기존 EvaluationResponse 에 JSON 컬럼으로만 붙여
     * 목표 이력 추적을 가능하게 한다.
     */
    @Column(name = "goal_snapshot_json", columnDefinition = "TEXT")
    private String goalSnapshotJson;

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

    public void updateTargetGoalIds(String targetGoalIdsJson) {
        this.targetGoalIdsJson = targetGoalIdsJson;
    }

    public void updateNormalizedScore(java.math.BigDecimal score) {
        this.normalizedScore = score;
    }

    public void updateGoalSnapshot(String goalSnapshotJson) {
        this.goalSnapshotJson = goalSnapshotJson;
    }
}
