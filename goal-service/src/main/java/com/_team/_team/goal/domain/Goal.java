package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @JoinColumn(name = "kpi_template_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private KpiTemplate kpiTemplate;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_goal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Goal parentGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private GoalOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
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

    public void activate(UUID approvedBy) {
        this.status = GoalStatus.ACTIVE;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = GoalStatus.COMPLETED;
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
}
