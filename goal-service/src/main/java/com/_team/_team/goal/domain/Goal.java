package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Goal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID goalId;

    @Column(nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_goal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Goal parentGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private GoalCycle cycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalOwnerType ownerType;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal baseline = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Column(precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal achievementPct = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAutoUpdated = false;

    @Enumerated(EnumType.STRING)
    private GoalProgressStatus progressStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoalStatus goalStatus = GoalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoalVisibility visibility = GoalVisibility.PRIVATE;

    @Column
    private UUID approvedBy;

    private java.time.LocalDateTime approvedAt;

    public void validateBusinessRule() {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("목표 종료일은 시작일보다 이후여야 합니다.");
        }
        if (targetValue.compareTo(baseline) == 0) {
            throw new IllegalArgumentException("목표값은 기준값과 같을 수 없습니다.");
        }
    }

    public void updateCheckin(BigDecimal actualValue, GoalProgressStatus progressStatus) {
        if (Boolean.TRUE.equals(isAutoUpdated)) {
            throw new IllegalStateException("자동 업데이트 목표는 actual_value를 직접 수정할 수 없습니다.");
        }
        this.actualValue = actualValue;
        this.progressStatus = progressStatus;
        this.achievementPct = calculateAchievementPct();
    }

    private BigDecimal calculateAchievementPct() {
        BigDecimal denominator = targetValue.subtract(baseline);
        if (denominator.compareTo(BigDecimal.ZERO) == 0 || actualValue == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal numerator = actualValue.subtract(baseline);
        return numerator
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }
}
