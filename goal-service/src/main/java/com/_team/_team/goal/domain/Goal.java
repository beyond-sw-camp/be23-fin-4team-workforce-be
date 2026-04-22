package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.converter.UuidListConverter;
import com._team._team.goal.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Goal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "goal_id")
    private UUID goalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_template_id", nullable = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private KpiTemplate kpiTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", length = 32)
    private KpiCycle cycle;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_kind", nullable = false, length = 32)
    @Builder.Default
    private GoalKind goalKind = GoalKind.KR;

    @Column(name = "auto_update", nullable = false)
    @Builder.Default
    private boolean autoUpdate = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 32)
    @Builder.Default
    private GoalHealthStatus healthStatus = GoalHealthStatus.NOT_STARTED;

    /** 팀 UUID 목록 (일부 공개) — DB에는 JSON 배열로 저장 */
    @Convert(converter = UuidListConverter.class)
    @Column(name = "visible_team_ids_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<UUID> visibleTeamIds = new ArrayList<>();

    /** 참여 멤버 UUID 목록 — DB에는 JSON 배열로 저장 */
    @Convert(converter = UuidListConverter.class)
    @Column(name = "participant_member_ids_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<UUID> participantMemberIds = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_goal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Goal parentGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private GoalOwnerType ownerType;

    /**
     * owner_type == MEMBER : 소유 사원의 memberId
     * owner_type == TEAM   : 소유 조직의 organizationId (사람이 아니라 조직 UUID)
     */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /**
     * [TEAM 목표 책임자]
     *   - owner_type == TEAM 일 때만 사용.
     *   - 이 팀 목표의 "완료 승인 요청 주도자" — 진행률 편집 주도자이기도 함.
     *   - MEMBER 목표는 null (ownerId 가 곧 책임자이므로 중복 저장 불필요).
     *   - 팀장 교체/이전 시 PATCH /goal/{id}/responsible 로 변경.
     */
    @Column(name = "responsible_member_id")
    private UUID responsibleMemberId;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_type", nullable = false)
    private MeasureType measureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(name = "unit_label", length = 20)
    private String unitLabel;

    @Column(name = "baseline", precision = 18, scale = 4, nullable = false)
    private BigDecimal baseline;

    @Column(name = "target_value", precision = 18, scale = 4, nullable = false)
    private BigDecimal targetValue;

    @Column(name = "actual_value", precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Column(name = "achievement_pct", precision = 7, scale = 2)
    private BigDecimal achievementPct;

    @Column(name = "rolled_achievement_pct", precision = 7, scale = 2)
    private BigDecimal rolledAchievementPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "rollup_policy", nullable = false, length = 32)
    @Builder.Default
    private RollupPolicy rollupPolicy = RollupPolicy.CHILDREN_AVG;

    @Enumerated(EnumType.STRING)
    @Column(name = "rollup_source", nullable = false, length = 32)
    @Builder.Default
    private RollupSource rollupSource = RollupSource.SELF;

    @Column(name = "cap_pct", nullable = false)
    private int capPct;

    @Column(name = "contribution_pct", precision = 5, scale = 2)
    private BigDecimal contributionPct;

    @Column(name = "weight_pct", precision = 5, scale = 2)
    private BigDecimal weightPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private GoalVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private GoalStatus status = GoalStatus.DRAFT;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 목표 생성 시 지정한 "종료(완료) 승인자".
     * 초기 활성 승인과 완료 승인에 모두 기본값으로 사용된다.
     * 완료 승인 요청 시 요청자가 별도로 지정하지 않으면 이 값이 적용.
     * null 이면 완료 승인 요청 시 반드시 지정해야 한다.
     */
    @Column(name = "completion_approver_id")
    private UUID completionApproverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_approval_status", nullable = false, length = 32)
    @Builder.Default
    private GoalApprovalStatus goalApprovalStatus = GoalApprovalStatus.NOT_REQUESTED;

    public void activate(UUID approvedBy) {
        this.status = GoalStatus.ACTIVE;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        if (this.healthStatus == GoalHealthStatus.NOT_STARTED) {
            this.healthStatus = GoalHealthStatus.ON_TRACK;
        }
    }

    public void complete() {
        this.status = GoalStatus.COMPLETED;
        this.healthStatus = GoalHealthStatus.COMPLETED;
    }

    public void cancel() {
        this.status = GoalStatus.CANCELLED;
    }

    public void update(String title, String description, GoalVisibility visibility,
                       BigDecimal weightPct, BigDecimal contributionPct) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (visibility != null) this.visibility = visibility;
        if (weightPct != null) this.weightPct = weightPct;
        if (contributionPct != null) this.contributionPct = contributionPct;
    }

    public void updateExtended(String title, String description, GoalVisibility visibility,
                               BigDecimal weightPct, BigDecimal contributionPct,
                               GoalKind goalKind, Boolean autoUpdate, GoalHealthStatus healthStatus,
                               List<UUID> visibleTeamIds, List<UUID> participantMemberIds, KpiCycle cycle) {
        update(title, description, visibility, weightPct, contributionPct);
        if (goalKind != null) this.goalKind = goalKind;
        if (autoUpdate != null) {
            this.autoUpdate = autoUpdate;
            if (autoUpdate) {
                this.rollupPolicy = RollupPolicy.CHILDREN_AVG;
            }
        }
        if (healthStatus != null) this.healthStatus = healthStatus;
        if (visibleTeamIds != null) this.visibleTeamIds = new ArrayList<>(visibleTeamIds);
        if (participantMemberIds != null) this.participantMemberIds = new ArrayList<>(participantMemberIds);
        if (cycle != null) this.cycle = cycle;
    }

    public void applyManualProgress(BigDecimal progressPct, GoalHealthStatus health) {
        BigDecimal v = progressPct == null ? BigDecimal.ZERO : progressPct;
        if (v.compareTo(BigDecimal.valueOf(100)) > 0) {
            v = BigDecimal.valueOf(100);
        }
        if (v.compareTo(BigDecimal.ZERO) < 0) {
            v = BigDecimal.ZERO;
        }
        this.achievementPct = v.setScale(2, RoundingMode.HALF_UP);
        if (health != null) {
            this.healthStatus = health;
        }
        this.rollupSource = RollupSource.SELF;
    }

    public void updateActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;

        BigDecimal raw;
        switch (this.measureType) {
            case HIGHER_BETTER -> {
                BigDecimal denominator = this.targetValue.subtract(this.baseline);
                if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                    raw = BigDecimal.ZERO;
                } else {
                    raw = actualValue.subtract(this.baseline)
                            .divide(denominator, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }
            case LOWER_BETTER -> {
                BigDecimal denominator = this.baseline.subtract(this.targetValue);
                if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                    raw = BigDecimal.ZERO;
                } else {
                    raw = this.baseline.subtract(actualValue)
                            .divide(denominator, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }
            case TARGET_MATCH -> {
                if (this.targetValue.compareTo(BigDecimal.ZERO) == 0) {
                    raw = BigDecimal.ZERO;
                } else {
                    raw = BigDecimal.valueOf(100)
                            .subtract(actualValue.subtract(this.targetValue).abs()
                                    .divide(this.targetValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)));
                }
            }
            default -> raw = BigDecimal.ZERO;
        }

        BigDecimal cap = BigDecimal.valueOf(this.capPct);
        this.achievementPct = raw.min(cap).setScale(2, RoundingMode.HALF_UP);
    }

    public void updateParentGoal(Goal parentGoal) {
        this.parentGoal = parentGoal;
    }

    public void updateRollup(BigDecimal rolledAchievementPct, RollupSource rollupSource) {
        this.rolledAchievementPct = rolledAchievementPct;
        this.rollupSource = rollupSource;
    }

    public void updateRollupPolicy(RollupPolicy rollupPolicy) {
        if (rollupPolicy != null) {
            this.rollupPolicy = rollupPolicy;
        }
    }

    public void updateGoalApprovalStatus(GoalApprovalStatus goalApprovalStatus) {
        this.goalApprovalStatus = goalApprovalStatus;
    }

    /**
     * 목표 생성 이후에도 종료 승인자를 바꿀 수 있게 허용.
     * null 이면 "지정 해제" 로 간주 (이 경우 완료 제출 시 DTO 로 지정 필수).
     */
    public void updateCompletionApproverId(UUID approverId) {
        this.completionApproverId = approverId;
    }

    /**
     * TEAM 목표의 책임자 교체. MEMBER 목표는 의미 없으므로 호출부에서 가드 필요.
     */
    public void updateResponsibleMemberId(UUID responsibleMemberId) {
        this.responsibleMemberId = responsibleMemberId;
    }
}
